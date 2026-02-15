package secs.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.itmo.secs.FileServiceApplication;
import org.itmo.secs.model.dto.FileDto;
import org.itmo.secs.repositories.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import secs.TestMultipartUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = FileServiceApplication.class
)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
class FileControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private FileRepository fileRepository;

    /* ===================== TESTCONTAINERS ===================== */

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("password")
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void registerPg(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Disable Liquibase for tests
        registry.add("spring.liquibase.enabled", () -> "false");

        // Application properties
        registry.add("app.max-page-size", () -> "10");
        registry.add("app.default-page-size", () -> "5");

        // JWT properties - use a simpler key for tests
        registry.add("token.signing.key",
                () -> "c2VjcmV0LWtleS1mb3ItdGVzdGluZy0xMjM0NTY3ODkwMTIzNDU2Nzg5MA==");
        registry.add("token.validity.period", () -> "3600000");

        // Disable Eureka client completely
        registry.add("eureka.client.enabled", () -> false);
        registry.add("eureka.client.fetch-registry", () -> false);
        registry.add("eureka.client.register-with-eureka", () -> false);

        // Disable other Spring Cloud features
        registry.add("spring.cloud.config.enabled", () -> false);
        registry.add("spring.cloud.config.import-check.enabled", () -> false);
        registry.add("spring.cloud.service-registry.auto-registration.enabled", () -> false);
    }

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        fileRepository.deleteAll();

        userToken = jwt(1L, "TestUser", "USER");
        adminToken = jwt(2L, "Admin", "ADMIN");

    }

    private String jwt(Long id, String name, String role) {
        return Jwts.builder()
                .subject(name)
                .claim("id", id)
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(
                        Decoders.BASE64.decode(
                                "c2VjcmV0LWtleS1mb3ItdGVzdGluZy0xMjM0NTY3ODkwMTIzNDU2Nzg5MA=="
                        )
                ))
                .compact();
    }

    /* ===================== UPLOAD ===================== */

    @Test
    void uploadFile_success() {

        FileDto dto = webTestClient.post()
                .uri("/file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .bodyValue(TestMultipartUtils.file("file", "test.txt", "HELLO"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FileDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(dto).isNotNull();
        assertThat(dto.fileName()).isEqualTo("test.txt");

        assertThat(
                fileRepository.findById(Objects.requireNonNull(dto).id())
        ).isNotNull();
    }

    /* ===================== METADATA ===================== */

    @Test
    void getFileMetadata_success() {

        FileDto created = upload();

        FileDto found = webTestClient.get()
                .uri("/file?id=" + created.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FileDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(found).isNotNull();
        assertThat(found.id()).isEqualTo(created.id());
    }

    @Test
    void getFileMetadata_notFound() {

        webTestClient.get()
                .uri("/file?id=999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    /* ===================== DOWNLOAD ===================== */

    @Test
    void downloadFile_success() {

        FileDto created = upload();

        byte[] bytes = webTestClient.get()
                .uri("/file/download?id=" + created.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        assertThat(new String(bytes, StandardCharsets.UTF_8))
                .isEqualTo("HELLO");
    }

    @Test
    void downloadFile_notFound() {

        webTestClient.get()
                .uri("/file/download?id=999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    /* ===================== HELPERS ===================== */

    private FileDto upload() {

        return webTestClient.post()
                .uri("/file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .bodyValue(TestMultipartUtils.file("file", "test.txt", "HELLO"))
                .exchange()
                .expectBody(FileDto.class)
                .returnResult()
                .getResponseBody();
    }
}
