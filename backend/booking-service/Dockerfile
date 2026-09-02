# Fase di build: serve il JDK completo solo per compilare. mvnw scarica da
# solo sia Maven che le dipendenze (maven-wrapper.jar non e' committato, vedi
# .gitignore), quindi questa fase ha bisogno di rete.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# pom.xml e il wrapper copiati PRIMA di src/: cosi' un cambio al solo codice
# non invalida la cache Docker sul layer delle dipendenze gia' scaricate.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Immagine finale: solo JRE, niente toolchain di build ne' sorgenti - piu'
# leggera e con meno superficie d'attacco della fase precedente.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non gira come root: se qualcuno sfrutta una vulnerabilita' nell'app, non si
# ritrova comunque root dentro il container.
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
