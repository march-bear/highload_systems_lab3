package secs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.itmo.secs.App;
import org.itmo.secs.client.DishServiceClient;
import org.itmo.secs.client.UserServiceClient;
import org.itmo.secs.model.dto.*;
import org.itmo.secs.model.entities.enums.Meal;
import org.itmo.secs.repositories.MenuRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = App.class
)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
class MenuControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private MenuRepository menuRepository;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private DishServiceClient dishServiceClient;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("password")
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void registerPg(DynamicPropertyRegistry r) {
        r.add("spring.r2dbc.url",
                () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":" +
                        POSTGRES.getMappedPort(5432) + "/testdb");
        r.add("spring.r2dbc.username", POSTGRES::getUsername);
        r.add("spring.r2dbc.password", POSTGRES::getPassword);

        r.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        r.add("spring.flyway.user", POSTGRES::getUsername);
        r.add("spring.flyway.password", POSTGRES::getPassword);
        r.add("spring.flyway.enabled", () -> true);

        r.add("eureka.client.enabled", () -> false);
        r.add("spring.cloud.config.enabled", () -> false);

        r.add("app.max-page-size", () -> "10");
        r.add("app.default-page-size", () -> "5");

        r.add("token.signing.key",
                () -> "WXzaS29yZ2V0VGVzdEtleTEyMzQ1Njc4OTA9Ozs7Ozs7Ozs7Ozs7Ozs7Owo=");
    }

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private String userToken;
    private String adminToken;
    private MenuCreateDto createDto;

    @BeforeEach
    void setUp() {
        menuRepository.deleteAll().block();

        userToken = jwt(1L, "TestUser", "USER");
        adminToken = jwt(2L, "Admin", "ADMIN");

        when(userServiceClient.getByName(any(), eq("TestUser")))
                .thenReturn(Mono.just(new UserDto(1L, "TestUser")));
        when(userServiceClient.getByName(any(), eq("NonExistent")))
                .thenReturn(Mono.empty());

        when(dishServiceClient.getById(eq(100L)))
                .thenReturn(Mono.just(new DishDto(100L, "Dish", 100, 10, 10, 10)));
        when(dishServiceClient.getById(eq(999L)))
                .thenReturn(Mono.empty());

        createDto = new MenuCreateDto(Meal.BREAKFAST.toString(), LocalDate.now());
    }

    private String jwt(Long id, String name, String role) {
        return Jwts.builder()
                .subject(name)
                .claim("id", id)
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(
                        Decoders.BASE64.decode(
                                "WXzaS29yZ2V0VGVzdEtleTEyMzQ1Njc4OTA9Ozs7Ozs7Ozs7Ozs7Ozs7Owo="
                        )
                ))
                .compact();
    }

    /* ===================== AUTH ===================== */

    @Test
    void createMenu_unauthorized() {
        webTestClient.post()
                .uri("/menu")
                .bodyValue(createDto)
                .exchange()
                .expectStatus().isUnauthorized();
    }

//    @Test
//    void createMenu_invalidToken_401() {
//        webTestClient.post()
//                .uri("/menu")
//                .header(HttpHeaders.AUTHORIZATION, "Bearer bad.token.ccc")
//                .bodyValue(createDto)
//                .exchange()
//                .expectStatus().isUnauthorized();
//    }

    /* ===================== CRUD ===================== */

    @Test
    void createMenu_success() {
        MenuDto dto = webTestClient.post()
                .uri("/menu")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MenuDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(dto).isNotNull();
        assertThat(dto.meal()).isEqualTo("BREAKFAST");
    }

    @Test
    void createMenu_duplicate_400() {
        webTestClient.post()
                .uri("/menu")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .bodyValue(createDto)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/menu")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .bodyValue(createDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void findMenuById_success() throws Exception {
        MenuDto created = create();

        MenuDto found = getMenu(created.id());

        assertThat(found.id()).isEqualTo(created.id());
    }

    @Test
    void deleteMenu_success() {
        MenuDto created = create();

        webTestClient.delete()
                .uri("/menu?id=" + created.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    /* ===================== FIND ===================== */

    @Test
    void findMenusByUsername_adminOnly() {
        create();

        webTestClient.get()
                .uri("/menu?username=TestUser")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void findMenusByUsername_userForbidden() {
        webTestClient.get()
                .uri("/menu?username=TestUser")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    /* ===================== DISHES ===================== */

//    @Test
//    void addAndDeleteDish_success() throws Exception {
//        MenuDto menu = create();
//
//        webTestClient.put()
//                .uri("/menu/dishes")
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
//                .bodyValue(new MenuDishDto(menu.id(), 100L))
//                .exchange()
//                .expectStatus().isNoContent();
//
//        List<DishDto> dishes = getDishes(menu.id());
//        assertThat(dishes).hasSize(1);
//
//        webTestClient.delete()
//                .uri(uri -> uri.path("/menu/dishes")
//                        .queryParam("id", menu.id())
//                        .build())
//                .exchange()
//                .expectStatus().isOk();
//
//        assertThat(getDishes(menu.id())).isEmpty();
//    }

    /* ===================== HELPERS ===================== */

    private MenuDto create() {
        return webTestClient.post()
                .uri("/menu")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .bodyValue(createDto)
                .exchange()
                .expectBody(MenuDto.class)
                .returnResult()
                .getResponseBody();
    }

    private MenuDto getMenu(Long id) throws Exception {
        String json = webTestClient.get()
                .uri("/menu?id=" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        return mapper.readValue(json, MenuDto.class);
    }

    private List<DishDto> getDishes(Long menuId) throws Exception {
        String json = webTestClient.get()
                .uri("/menu/dishes?id=" + menuId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        return mapper.readValue(
                json,
                mapper.getTypeFactory().constructCollectionType(List.class, DishDto.class)
        );
    }
}
