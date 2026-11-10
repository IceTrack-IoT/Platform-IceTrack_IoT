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

# JVM sizing for a small container. Without these flags the JVM sizes itself from the memory it
# sees, and on a shared host that can be far more than the container is billed for. A serial
# collector and a capped heap keep the resident memory near 300 MB. Override JAVA_OPTS from the
# environment (e.g. -XX:MaxRAMPercentage=60) when the instance gets more memory.
ENV JAVA_OPTS="-XX:+UseSerialGC -XX:MaxRAMPercentage=60 -XX:MaxMetaspaceSize=160m -Xss512k -XX:ReservedCodeCacheSize=64m -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT exec java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} -jar app.jar
