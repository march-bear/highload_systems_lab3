FROM gradle:jdk21 as build
WORKDIR /file-service

COPY build.gradle.kts ./

RUN --mount=type=cache,id=gradle-cache-file-service,target=/home/gradle/.gradle \
    gradle help --no-daemon

COPY src src

RUN --mount=type=cache,id=gradle-cache-file-service,target=/home/gradle/.gradle \
    gradle build -x test --no-daemon

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /file-service/build/libs/*.jar file-service.jar
ENTRYPOINT ["java","-jar","file-service.jar"]