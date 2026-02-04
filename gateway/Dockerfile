FROM gradle:jdk21 as build
WORKDIR /gateway

COPY build.gradle.kts ./

RUN --mount=type=cache,id=gradle-cache-gateway,target=/home/gradle/.gradle \
    gradle help --no-daemon

COPY src src

RUN --mount=type=cache,id=gradle-cache-gateway,target=/home/gradle/.gradle \
    gradle build -x test --no-daemon

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /gateway/build/libs/*.jar gateway.jar
ENTRYPOINT ["java","-jar","gateway.jar"]