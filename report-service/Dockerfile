# ================================
# Stage 1: Build (ROOT CONTEXT)
# ================================
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 🔥 ROOT PROJECT COPY (parent POM shu yerda)
COPY ../../ /app

# faqat report-service build
RUN mvn -pl report-service -am clean package -DskipTests -q


# ================================
# Stage 2: Run
# ================================
FROM --platform=linux/amd64 eclipse-temurin:17-jre

WORKDIR /app

# JAR copy
COPY --from=builder /app/report-service/target/report-service-1.0.0.jar /app/report-service.jar

# Entrypoint
ENTRYPOINT ["java", "-jar", "/app/report-service.jar"]
