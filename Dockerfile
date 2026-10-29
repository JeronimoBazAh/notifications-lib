# Stage 1: build the library and run the unit tests
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package

# Stage 2: slim runtime that only runs the demo (all providers are simulated)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/notifications-lib-1.0.0.jar app.jar
CMD ["java", "-jar", "app.jar"]
