# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY orderservice.api/ orderservice.api/
COPY orderservice.api.contracts/ orderservice.api.contracts/
COPY orderservice.application/ orderservice.application/
COPY orderservice.domain/ orderservice.domain/
COPY orderservice.domain.messaging.models/ orderservice.domain.messaging.models/
COPY orderservice.infrastructure.db/ orderservice.infrastructure.db/
COPY orderservice.infrastructure.gateway/ orderservice.infrastructure.gateway/
COPY orderservice.infrastructure.messaging.publisher/ orderservice.infrastructure.messaging.publisher/
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -q package -DskipTests \
    && cp orderservice.api/target/orderservice.api-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar \
       extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --no-create-home spring
USER spring

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
