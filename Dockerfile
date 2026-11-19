# syntax=docker/dockerfile:1

# ---------- build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# The wrapper is pinned to Maven 3.9.9 in .mvn/wrapper/maven-wrapper.properties.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S -g 10001 app \
    && adduser -S -u 10001 -G app app

WORKDIR /app
COPY --from=build /workspace/target/quarkus-app/ /app/
RUN chown -R 10001:10001 /app

USER 10001:10001
EXPOSE 8080
STOPSIGNAL SIGTERM

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/q/health/live || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/quarkus-run.jar"]

LABEL org.opencontainers.image.title="order-service" \
      org.opencontainers.image.description="Java 21 + Quarkus order management API" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.source="https://github.com/aryafatthurahman/backend-service-2026"
