# Fase di build con Maven e cache delle dipendenze
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Fase di runtime leggera con JRE e utente non-root (richiesto dall'audit)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd -ms /bin/bash appuser && chown -R appuser:appuser /app
USER appuser
COPY --chown=appuser:appuser --from=builder /build/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]