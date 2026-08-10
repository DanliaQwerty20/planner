# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src src
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S planner && adduser -S planner -G planner
COPY --from=build --chown=planner:planner /workspace/build/libs/planner.jar /app/planner.jar

USER planner
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/planner.jar"]
