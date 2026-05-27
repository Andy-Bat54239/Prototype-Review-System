# Multi-stage build: build the whole workspace once, run only this service's jar.
# Build context is prbs-services/ (the parent directory), per docker-compose.yml.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY prbs-shared ./prbs-shared
COPY eureka-server ./eureka-server
COPY api-gateway ./api-gateway
COPY auth-service ./auth-service
COPY user-service ./user-service
COPY booking-service ./booking-service
COPY notification-service ./notification-service
RUN mvn -B -DskipTests -pl eureka-server -am package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/eureka-server/target/*.jar /app/app.jar
EXPOSE 8761
ENTRYPOINT ["java","-jar","/app/app.jar"]
