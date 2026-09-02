# --- Build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# --- Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non gira come root: se qualcuno sfrutta una vulnerabilita' nell'app, non si
# ritrova comunque root dentro il container.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /build/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
