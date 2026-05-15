# ================================
# Stage 1: Build (ROOT CONTEXT)
# ================================
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 🔥 ROOT PROJECT COPY (parent POM shu yerda)
COPY ../../ /app

# faqat order-service build
RUN mvn -pl order-service -am clean package -DskipTests -q


# ================================
# Stage 2: Run
# ================================
FROM --platform=linux/amd64 eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# jar path MUHIM
COPY --from=builder /app/order-service/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

ENV JVM_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]