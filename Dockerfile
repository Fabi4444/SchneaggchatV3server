FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy only dependency-related files first
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Download dependencies (this layer will be cached)
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# Now copy source code (changes here won't invalidate dependency cache)
COPY src src

# Build the application
RUN ./gradlew clean build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]