plugins {
    id("java")
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "org.itmo.secs"
version = "1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("org.springframework.boot:spring-boot-starter-security:3.5.8")
    implementation("org.springframework.kafka:spring-kafka")

    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j:5.0.1")
    implementation("org.liquibase:liquibase-core:5.0.1")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-api:2.8.14")
    implementation("org.postgresql:r2dbc-postgresql:1.1.1.RELEASE")
    implementation("io.projectreactor:reactor-core:3.8.2")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("com.playtika.reactivefeign:feign-reactor-spring-cloud-starter:4.2.1")

    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.flywaydb:flyway-core")

    runtimeOnly("org.postgresql:postgresql")


    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.flywaydb:flyway-core")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.mockito:mockito-core:2.1.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("io.rest-assured:rest-assured:5.5.6")
    testImplementation("com.google.code.gson:gson:2.13.2")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
    }
}

tasks.test {
    useJUnitPlatform()
}
