import org.itmo.user.accounter.UserAccounterApp;
import org.itmo.user.accounter.model.dto.UserAuthDto;
import org.itmo.user.accounter.model.dto.JwtTokenDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import reactor.core.publisher.Mono;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = UserAccounterApp.class)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
class UserControllerTest {
    @LocalServerPort
    private String port;

    @Autowired
    private WebTestClient webTestClient;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("postgres")
                    .withPassword("password")
                    .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        //r2dbc props
        String jdbcUrl = POSTGRES.getJdbcUrl();
        registry.add("spring.r2dbc.url",
                () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":" +
                        POSTGRES.getMappedPort(5432) +
                        "/testdb");
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        // jdbc liquibase props
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> true);

        registry.add("token.signing.key",
                () -> "dGVzdC10b2tlbi1zaWduaW5nLWtleS10ZXN0LXRva2Vu");
        registry.add("token.validity.period",
                () -> "3600000");
    }

    // Helper method to get admin token
    private String getAdminToken() {
        // First, create an admin user (you might need to seed your test database with an admin user)
        // Or create a regular user and use it for tests that don't require admin role
        return getToken("ffff", "password");
    }

    private String registerAndGetToken(String username, String password) {
        UserAuthDto authRequest = new UserAuthDto(username, password);

        JwtTokenDto tokenDto = webTestClient.post()
                .uri("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JwtTokenDto.class)
                .returnResult()
                .getResponseBody();

        return tokenDto != null ? tokenDto.token() : null;
    }

    private String getToken(String username, String password) {
        UserAuthDto authRequest = new UserAuthDto(username, password);

        JwtTokenDto tokenDto = webTestClient.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(JwtTokenDto.class)
                .returnResult()
                .getResponseBody();

        return tokenDto != null ? tokenDto.token() : null;
    }

    /* ---------------- CREATE ---------------- */

    @Test
    void createUser_unauthorizedWhenNoToken() {
        webTestClient.post()
                .uri("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();
    }


    @Test
    void findUser_badRequest() {
        String token = getAdminToken();

        webTestClient.get()
                .uri("/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isBadRequest(); // No id or name parameter
    }

    @Test
    void whoami_authenticated() {
        String username = "whoamiUser";
        String password = "password123";

        // Register
        registerAndGetToken(username, password);

        // Login
        String token = getToken(username, password);

        webTestClient.get()
                .uri("/user/whoami")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo(username);
    }

    @Test
    void whoami_unauthenticated() {
        webTestClient.get()
                .uri("/user/whoami")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /* ---------------- AUTH TESTS ---------------- */

    @Test
    void register_success() {
        UserAuthDto request = new UserAuthDto("newUser", "password123");

        webTestClient.post()
                .uri("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JwtTokenDto.class)
                .value(dto -> {
                    assert dto.token() != null;
                    assert !dto.token().isEmpty();
                });
    }

    @Test
    void login_success() {
        // First register
        String username = "loginUser";
        String password = "password123";
        registerAndGetToken(username, password);

        // Then login
        UserAuthDto request = new UserAuthDto(username, password);

        webTestClient.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(JwtTokenDto.class)
                .value(dto -> {
                    assert dto.token() != null;
                    assert !dto.token().isEmpty();
                });
    }

    @Test
    void login_invalidCredentials() {
        UserAuthDto request = new UserAuthDto("nonexistent", "wrongpassword");

        webTestClient.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    /* ---------------- UPDATE ROLE ---------------- */

    @Test
    void updateUserRole_success() {
        String token = registerAndGetToken("roleUser", "password123");
    }

    /* ---------------- DELETE ---------------- */

    @Test
    void deleteUser_success() {
        String username = "deleteUser";
        String password = "password123";

        String token = registerAndGetToken(username, password);
    }
}