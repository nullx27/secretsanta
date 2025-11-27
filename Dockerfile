# Build frontend assets
FROM node:20-alpine AS frontend-builder
WORKDIR /app
COPY frontend/package*.json frontend/
COPY frontend frontend
RUN cd frontend \
    && npm install \
    && npm run build

# Build Spring Boot jar
FROM eclipse-temurin:21-jdk AS backend-builder
WORKDIR /app
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src
# Include built frontend assets
COPY --from=frontend-builder /app/src/main/resources/static src/main/resources/static
RUN ./gradlew --no-daemon bootJar

# Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080
COPY --from=backend-builder /app/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
