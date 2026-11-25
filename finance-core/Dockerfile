# ---- build stage: compile the jar with Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- run stage: small Java 21 runtime only ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8080
# Render tells the app which port to use via $PORT; cap memory for the free 512MB instance
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT:-8080} -jar app.jar"]