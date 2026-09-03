# --- Build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# --- Runtime ---
# 17-jre (non -alpine): l'immagine Alpine di Temurin non ha una build ARM64,
# quindi il build fallirebbe su Apple Silicon.
FROM eclipse-temurin:17-jre
WORKDIR /app

# Non gira come root: se qualcuno sfrutta una vulnerabilita' nell'app, non si
# ritrova comunque root dentro il container.
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /build/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
