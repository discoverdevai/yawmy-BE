# syntax=docker/dockerfile:1.7

# ---------- Stage 1: build with Maven --------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy POM first so dependency download is cached across source-only changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy sources and build the fat jar (skip tests — they need only H2)
COPY src ./src
RUN mvn -B -q -DskipTests package \
    && mv target/*.jar /workspace/app.jar

# ---------- Stage 2: minimal runtime ---------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for safety
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /workspace/app.jar /app/app.jar

# Render injects $PORT (defaults to 10000 on their Web Services).
# Fall back to 8080 for local `docker run`.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar --server.port=${PORT:-8080}"]
