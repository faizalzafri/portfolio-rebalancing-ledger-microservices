# Stage 1: Build the application using Maven and Temurin JDK 21
FROM maven:3.9.7-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Compile and package the application, skipping tests for velocity
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image containing only the JRE and Jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the compiled jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Define startup command
ENTRYPOINT ["java", "-jar", "app.jar"]
