FROM gradle:jdk21 as build
WORKDIR /user-accounter

COPY build.gradle.kts ./

RUN --mount=type=cache,id=gradle-cache-user-accounter,target=/home/gradle/.gradle \
    gradle help --no-daemon

COPY src src

RUN --mount=type=cache,id=gradle-cache-user-accounter,target=/home/gradle/.gradle \
    gradle build -x test --no-daemon

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /user-accounter/build/libs/*.jar user-accounter.jar
ENTRYPOINT ["java","-jar","user-accounter.jar"]