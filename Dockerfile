FROM gradle:8.8.0-jdk17 as builder

WORKDIR /app
COPY . .
COPY gradle.properties /root/gradle.properties
RUN gradle bootJar

FROM eclipse-temurin:17-jre-alpine as prod

RUN apk add --no-cache \
    freetype \
    fontconfig \
    ttf-dejavu

COPY --from=builder /app/build/libs/ProBackend*.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
