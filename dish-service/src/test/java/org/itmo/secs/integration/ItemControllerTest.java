package org.itmo.secs.integration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.itmo.secs.model.dto.ItemCreateDto;
import org.itmo.secs.model.dto.ItemUpdateDto;
import org.itmo.secs.model.entities.Item;
import org.itmo.secs.notification.DishEventProducer;
import org.itmo.secs.notification.ItemEventProducer;
import org.itmo.secs.repositories.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.restassured.RestAssured;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ItemControllerTest {
    private static final String TEST_JWT = Jwts.builder()
            .subject("test-user")
            .claim("id", 1L)
            .claim("role", "ADMIN")
            .claim("authorities", List.of("ADMIN"))
            .expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(Keys.hmacShaKeyFor(
                    Decoders.BASE64.decode("WXzaS29yZ2V0VGVzdEtleTEyMzQ1Njc4OTA9Ozs7Ozs7Ozs7Ozs7Ozs7Owo")
            ))
            .compact();

    @LocalServerPort
    private String port;

    @MockitoBean
    private ItemEventProducer itemEventProducer;

    @Container
    static PostgreSQLContainer<?> pgContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test-db")
            .withUsername("postgres")
            .withPassword("password");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl);
        registry.add("spring.datasource.username", pgContainer::getUsername);
        registry.add("spring.datasource.password", pgContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // Отключаем Liquibase для тестов
        registry.add("spring.liquibase.enabled", () -> "false");
        // Отключаем конфиг сервер для тестов
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.cloud.config.import-check.enabled", () -> "false");
        registry.add("app.max-page-size", () -> "10");
        registry.add("app.default-page-size", () -> "5");
        registry.add("token.signing.key",
                () -> "WXzaS29yZ2V0VGVzdEtleTEyMzQ1Njc4OTA9Ozs7Ozs7Ozs7Ozs7Ozs7Owo=");

    }

    @Autowired
    private ItemRepository itemRepository;

    private List<Item> items;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + port;
        RestAssured.port = Integer.parseInt(port);

        itemRepository.deleteAll();

        items = itemRepository.saveAll(List.of(
                new Item(null, "Item1", 300, 50, 20, 10, new ArrayList<>()),
                new Item(null, "Item2", 320, 55, 22, 12, new ArrayList<>())
        ));
    }

    @Test
    void testCreateNewItem() {
        ItemCreateDto dto =
                new ItemCreateDto("NEW_ITEM", 400, 60, 25, 15);

        RestAssured.given()
                .header("Authorization", "Bearer " + TEST_JWT)
                .contentType("application/json")
                .body(new Gson().toJson(dto))
                .post("/item")
                .then()
                .statusCode(201);

        assertTrue(itemRepository.findByName("NEW_ITEM").isPresent());
    }

    @Test
    void createItem_WithNameTooShort_ShouldReturn400() {
        ItemCreateDto dto = new ItemCreateDto(
                "AB",
                300, 50, 20, 10
        );

        RestAssured.given()
                .header("Authorization", "Bearer " + TEST_JWT)
                .contentType("application/json")
                .body(new Gson().toJson(dto))
                .post("/item")
                .then()
                .statusCode(400);

        assertFalse(itemRepository.findByName("AB").isPresent());
    }

    @Test
    void testUpdate() {
        Item item = items.get(0);

        ItemUpdateDto dto = new ItemUpdateDto(
                item.getId(), "UPDATED", 500, 70, 30, 20
        );

        RestAssured.given()
                .header("Authorization", "Bearer " + TEST_JWT)
                .contentType("application/json")
                .body(new Gson().toJson(dto))
                .put("/item")
                .then()
                .statusCode(204);
    }

    @Test
    void testDelete() {
        Item item = items.get(0);

        RestAssured.given()
                .header("Authorization", "Bearer " + TEST_JWT)
                .param("id", item.getId())
                .delete("/item")
                .then()
                .statusCode(204);

        assertFalse(itemRepository.existsById(item.getId()));
    }

    @Test
    void testFindById_success() {
        Item item = items.get(0);

        RestAssured.given()
                .header("Authorization", "Bearer " + TEST_JWT)
                .param("id", item.getId())
                .when()
                .get("/item")
                .then()
                .statusCode(200)
                .body("[0].name", equalTo(item.getName()));
    }

    @Test
    void testFindByName_success() {
        Item item = items.get(0);

        RestAssured.given()
                .header("Authorization", "Bearer " + TEST_JWT)
                .param("name", item.getName())
                .when()
                .get("/item")
                .then()
                .statusCode(200)
                .body("[0].name", equalTo(item.getName()));
    }

    @Test
    void testFindAll_defaultPagination() {
        RestAssured.given()
                .header("Authorization", "Bearer " + TEST_JWT)
                .when()
                .get("/item")
                .then()
                .statusCode(200)
                .header("X-Total-Count", String.valueOf(items.size()))
                .body("size()", equalTo(items.size()));
    }
}