# syntax=docker/dockerfile:1

FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /build
COPY . .
RUN mvn -B -ntp -pl starter-schema liquibase:help
ENTRYPOINT ["sh", "-c", "mvn -B -ntp -o liquibase:update -pl starter-schema \
            -Dliquibase.url=${LIQUIBASE_URL:-jdbc:postgresql://postgres:5432/app} \
            -Dliquibase.username=${LIQUIBASE_USERNAME:-app}"]
