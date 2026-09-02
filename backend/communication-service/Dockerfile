# Fase 1: Build (usa Maven per scaricare le dipendenze e compilare)
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# Copia il file delle dipendenze e il codice sorgente
COPY pom.xml .
COPY src ./src
# Compila il pacchetto ignorando i test
RUN mvn clean package -DskipTests

# Fase 2: Runtime (immagine dedicata solo all'esecuzione)
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
# Preleva esclusivamente il .jar generato dalla Fase 1
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]