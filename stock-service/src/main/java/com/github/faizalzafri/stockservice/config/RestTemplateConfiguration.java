package com.github.faizalzafri.stockservice.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfiguration {

    private ObjectMapper strictObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true); // Fail if JSON contains extra fields
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);          // Fail on empty Java objects
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);     // Sort fields alphabetically
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);    // Sort map keys alphabetically
        mapper.registerModule(new JavaTimeModule());                               // Support Java 8 Time types
        return mapper;
    }

    private void configureStrictJackson(RestTemplate restTemplate) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(strictObjectMapper());
        
        // Remove standard Jackson message converter and replace with our strict serializer
        restTemplate.getMessageConverters().removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
        restTemplate.getMessageConverters().add(0, converter);
    }

    /**
     * Load-balanced RestTemplate (used for internal Eureka microservice discovery routing).
     * Marked as @Primary so it's injected by default.
     */
    @Bean(name = "loadBalancedRestTemplate")
    @LoadBalanced
    @Primary
    public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3)) // 3s Connection Timeout
                .setReadTimeout(Duration.ofSeconds(5))    // 5s Read Timeout
                .build();
        configureStrictJackson(restTemplate);
        return restTemplate;
    }

    /**
     * Default RestTemplate (used for external REST API calls, bypassing Eureka load balancer).
     */
    @Bean(name = "defaultRestTemplate")
    public RestTemplate defaultRestTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3)) // 3s Connection Timeout
                .setReadTimeout(Duration.ofSeconds(5))    // 5s Read Timeout
                .build();
        configureStrictJackson(restTemplate);
        return restTemplate;
    }
}
