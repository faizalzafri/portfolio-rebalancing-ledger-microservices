package com.github.faizalzafri.rebalancing.config;

import com.github.faizalzafri.rebalancing.model.PortfolioDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class RestTemplateConfigurationTest {

    @Autowired
    @Qualifier("defaultRestTemplate")
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testStrictJacksonDeserialization_ShouldFailOnUnknownProperty() {
        // given
        String jsonWithUnknownProperties = "{\"id\":100, \"name\":\"Retirement Portfolio\", \"garbageField\":\"unwanted-data\"}";

        mockServer.expect(requestTo("/test-endpoint"))
                .andRespond(withSuccess(jsonWithUnknownProperties, MediaType.APPLICATION_JSON));

        // then
        assertThrows(RestClientException.class, () -> {
            restTemplate.getForObject("/test-endpoint", PortfolioDto.class);
        });

        mockServer.verify();
    }

    @Test
    public void testStrictJacksonDeserialization_ShouldSucceedOnValidProperties() {
        // given
        String validJson = "{\"id\":100, \"name\":\"Retirement Portfolio\", \"username\":\"faiz\"}";

        mockServer.expect(requestTo("/test-endpoint-valid"))
                .andRespond(withSuccess(validJson, MediaType.APPLICATION_JSON));

        // when
        PortfolioDto result = restTemplate.getForObject("/test-endpoint-valid", PortfolioDto.class);

        // then
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Retirement Portfolio", result.getName());
        assertEquals("faiz", result.getUsername());

        mockServer.verify();
    }
}
