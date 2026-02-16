FROM gradle:jdk21 as build
WORKDIR /notification-service

COPY build.gradle.kts ./

RUN --mount=type=cache,id=gradle-cache-notification-service,target=/home/gradle/.gradle \
    gradle help --no-daemon

COPY src src

RUN --mount=type=cache,id=gradle-cache-notification-service,target=/home/gradle/.gradle \
    gradle build -x test --no-daemon

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /notification-service/build/libs/*.jar notification-service.jar
ENTRYPOINT ["java","-jar","notification-service.jar"]