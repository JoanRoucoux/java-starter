# syntax=docker/dockerfile:1

FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /build
COPY . .
RUN mvn -B -ntp -DskipTests package \
    && rm -f *-kafka/target/*.jar.original

FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /build/*-kafka/target/*.jar app.jar
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
