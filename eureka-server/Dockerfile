FROM gradle:jdk21 as build
WORKDIR /eureka-server

COPY build.gradle.kts ./

RUN --mount=type=cache,id=gradle-cache-eureka,target=/home/gradle/.gradle \
    gradle help --no-daemon

COPY src src

RUN --mount=type=cache,id=gradle-cache-eureka,target=/home/gradle/.gradle \
    gradle build -x test --no-daemon

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /eureka-server/build/libs/*.jar eureka-server.jar
ENTRYPOINT ["java","-jar","eureka-server.jar"]