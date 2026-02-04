FROM gradle:jdk21 as build
WORKDIR /menu-service

COPY build.gradle.kts ./

RUN --mount=type=cache,id=gradle-cache-menu-service,target=/home/gradle/.gradle \
    gradle help --no-daemon

COPY src src

RUN --mount=type=cache,id=gradle-cache-menu-service,target=/home/gradle/.gradle \
    gradle build -x test --no-daemon

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /menu-service/build/libs/*.jar menu-service.jar
ENTRYPOINT ["java","-jar","menu-service.jar"]