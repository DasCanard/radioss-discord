FROM gradle:8.10.2-jdk21 AS build

WORKDIR /app
COPY . .

RUN gradle shadowJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/radioss-discord-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]