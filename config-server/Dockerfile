FROM gradle:jdk21 as build
WORKDIR /config-server

COPY build.gradle.kts ./

RUN --mount=type=cache,id=gradle-cache-gateway,target=/home/gradle/.gradle \
    gradle help --no-daemon

COPY src src

RUN --mount=type=cache,id=gradle-cache-gateway,target=/home/gradle/.gradle \
    gradle build -x test --no-daemon

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /config-server/build/libs/*.jar config-server.jar
ENTRYPOINT ["java","-jar","config-server.jar"]