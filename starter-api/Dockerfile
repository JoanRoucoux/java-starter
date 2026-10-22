# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /build
COPY . .
RUN mvn -B -ntp -DskipTests package \
    && rm -f *-api/target/*.jar.original

FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /build/*-api/target/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD wget --spider -q http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
