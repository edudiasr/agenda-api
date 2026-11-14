# etapa 1: compila com o JDK completo
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# etapa 2: só o JRE e o .jar, rodando com usuário sem privilégio
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/agenda-api-*.jar app.jar
USER app
EXPOSE 8080
# limita o heap a 75% da memória do container (planos gratuitos costumam ter 512 MB)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
