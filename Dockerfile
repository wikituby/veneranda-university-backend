# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests
COPY src ./src
RUN mvn -B package -DskipTests -Dquarkus.package.jar.type=uber-jar

# Run stage — Render injects PORT; Quarkus reads it via application.yml (%prod)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*-runner.jar /app/app-runner.jar
ENV QUARKUS_PROFILE=prod
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app-runner.jar"]
