FROM gradle:jdk21 as build
WORKDIR /dish-service

COPY build.gradle.kts ./

RUN --mount=type=cache,id=gradle-cache-dish-service,target=/home/gradle/.gradle \
    gradle help --no-daemon

COPY src src

RUN --mount=type=cache,id=gradle-cache-dish-service,target=/home/gradle/.gradle \
    gradle build -x test --no-daemon

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /dish-service/build/libs/*.jar dish-service.jar
ENTRYPOINT ["java","-jar","dish-service.jar"]