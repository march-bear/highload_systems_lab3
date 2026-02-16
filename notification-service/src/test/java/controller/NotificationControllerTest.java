package controller;

import com.google.gson.Gson;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.restassured.RestAssured;
import org.itmo.secs.App;
import org.itmo.secs.model.dto.InfoDto;
import org.itmo.secs.model.dto.NotificationDto;
import org.itmo.secs.model.entities.Notification;
import org.itmo.secs.model.entities.enums.EventType;
import org.itmo.secs.repositories.NotificationRepository;
import org.itmo.secs.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@SpringBootTest(
        classes = App.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "kafka.enabled=false"
        }
)
@Testcontainers
@Transactional
class NotificationControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    private final Long USER_ID = 1L;

    private Notification notification;
    private NotificationDto notificationDto;

    @MockitoBean
    private KafkaTemplate<?, ?> kafkaTemplate;

    @Container
    static PostgreSQLContainer<?> pgContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test-db")
            .withUsername("postgres")
            .withPassword("password");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        // Database properties
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl);
        registry.add("spring.datasource.username", pgContainer::getUsername);
        registry.add("spring.datasource.password", pgContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Disable Liquibase for tests
        registry.add("spring.liquibase.enabled", () -> "false");

        // Application properties
        registry.add("app.max-page-size", () -> "10");
        registry.add("app.default-page-size", () -> "5");

        // JWT properties
        registry.add("token.signing.key",
                () -> "c2VjcmV0LWtleS1mb3ItdGVzdGluZy0xMjM0NTY3ODkwMTIzNDU2Nzg5MA==");
        registry.add("token.validity.period", () -> "3600000");

        // Disable security
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "");

        // Disable Eureka
        registry.add("eureka.client.enabled", () -> false);
        registry.add("eureka.client.fetch-registry", () -> false);
        registry.add("eureka.client.register-with-eureka", () -> false);


        // Disable other Spring Cloud features
        registry.add("spring.cloud.config.enabled", () -> false);
        registry.add("spring.cloud.config.import-check.enabled", () -> false);
        registry.add("spring.cloud.service-registry.auto-registration.enabled", () -> false);
    }

    private String adminToken;

    @BeforeEach
    void setup() {
        notificationRepository.deleteAll();
        RestAssured.baseURI = "http://localhost:" + port;

        // Create notification WITHOUT hardcoded ID
        notification = Notification.builder()
                .type(EventType.INFO)
                .title("title")
                .message("message")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .readAt(LocalDateTime.now())
                .userId(USER_ID)  // Associate with the test user
                .build();

        // Save to get generated ID
        notification = notificationService.save(notification).block();

        // Create DTO with the actual generated ID
        notificationDto = new NotificationDto(
                notification.getId(),
                "INFO",
                notification.getTitle(),
                notification.getMessage(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );

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

    @Test
    void shouldSendInfoNotification() {
        InfoDto dto = new InfoDto("title", "test message");

        RestAssured.given()
                .contentType("application/json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .body(new Gson().toJson(dto))
                .post("/notification")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldSetNotificationRead() {
        RestAssured.given()
                .header("X-User-Id", USER_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .queryParam("id", notification.getId())
                .put("/notification")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldSetAllNotificationsRead() {
        RestAssured.given()
                .header("X-User-Id", USER_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .put("/notification/all")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldFindNotificationById() {
        RestAssured.given()
                .header("X-User-Id", USER_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .queryParam("id", notification.getId())
                .get("/notification")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldFindAllNotifications() {
        RestAssured.given()
                .header("X-User-Id", USER_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .queryParam("unread-only", false)
                .get("/notification/all")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldFindAllByType() {
        RestAssured.given()
                .header("X-User-Id", USER_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .queryParam("type", "INFO")
                .queryParam("unread-only", false)
                .get("/notification/all")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldReturnErrorWhenTypeInvalid() {
        RestAssured.given()
                .header("X-User-Id", USER_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .queryParam("type", "WRONG")
                .queryParam("unread-only", false)
                .get("/notification/all")
                .then()
                .statusCode(400);
    }
}