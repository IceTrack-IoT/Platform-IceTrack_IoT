# Compilation Step
FROM maven:3.9.16-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Execution Step
FROM eclipse-temurin:26-jre-noble AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT exec java -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} -jar app.jar