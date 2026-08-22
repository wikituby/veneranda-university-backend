# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests
COPY src ./src
RUN mvn -B package -DskipTests -Dquarkus.package.jar.type=uber-jar

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*-runner.jar /app/app-runner.jar
ENV QUARKUS_PROFILE=prod
EXPOSE 8080
CMD ["java", "-jar", "/app/app-runner.jar"]
