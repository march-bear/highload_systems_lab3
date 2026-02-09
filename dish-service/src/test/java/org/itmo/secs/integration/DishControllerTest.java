package org.itmo.secs.integration;

import com.google.gson.Gson;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.itmo.secs.model.dto.*;
import org.itmo.secs.model.entities.Dish;
import org.itmo.secs.model.entities.Item;
import org.itmo.secs.repositories.DishRepository;
import org.itmo.secs.repositories.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@Testcontainers
class DishControllerTest {

    @LocalServerPort
    private int port;

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

        // JWT properties - use a simpler key for tests
        registry.add("token.signing.key",
                () -> "c2VjcmV0LWtleS1mb3ItdGVzdGluZy0xMjM0NTY3ODkwMTIzNDU2Nzg5MA==");
        registry.add("token.validity.period", () -> "3600000");

        // Disable security for tests if needed, or configure test security
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "");

        // Disable Eureka client completely
        registry.add("eureka.client.enabled", () -> false);
        registry.add("eureka.client.fetch-registry", () -> false);
        registry.add("eureka.client.register-with-eureka", () -> false);

        // Disable other Spring Cloud features
        registry.add("spring.cloud.config.enabled", () -> false);
        registry.add("spring.cloud.config.import-check.enabled", () -> false);
        registry.add("spring.cloud.service-registry.auto-registration.enabled", () -> false);
    }

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private DishRepository dishRepository;

    private List<Dish> dishes;
    private List<Item> items;
    private String testJwtToken;

    private String generateJwtToken() {
        try {
            String signingKeyBase64 = "c2VjcmV0LWtleS1mb3ItdGVzdGluZy0xMjM0NTY3ODkwMTIzNDU2Nzg5MA==";

            // Используйте правильную структуру JWT с правильными полями
            return Jwts.builder()
                    .subject("test-user")
                    .claim("id", 1L)
                    .claim("role", "ADMIN")  // Или "USER" в зависимости от требований
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(signingKeyBase64)))
                    .compact();
        } catch (Exception e) {
            // Fallback to a test token that might work
            return "eyJhbGciOiJIUzI1NiJ9." +
                    "eyJzdWIiOiJ0ZXN0LXVzZXIiLCJpZCI6MSwicm9sZSI6IkFETUlOIiwiZXhwIjoyMDAwMDAwMDAwfQ." +
                    "dummy-signature";
        }
    }

    @BeforeEach
    void setUp() {
        // Clear repositories before each test
        dishRepository.deleteAll();
        itemRepository.deleteAll();

        // Generate JWT token
        testJwtToken = generateJwtToken();

        // Initialize RestAssured
        io.restassured.RestAssured.baseURI = "http://localhost:" + port;
        io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Create test items
        items = new ArrayList<>();
        items.add(itemRepository.save(new Item(null, "Milk1", 300, 50, 20, 10, new ArrayList<>())));
        items.add(itemRepository.save(new Item(null, "Milk2", 300, 50, 20, 10, new ArrayList<>())));

        // Create test dishes
        dishes = new ArrayList<>();
        dishes.add(dishRepository.save(new Dish(null, "Dish1", new ArrayList<>())));
        dishes.add(dishRepository.save(new Dish(null, "Dish2", new ArrayList<>())));
    }

    @Test
    void testCreateNewDish() {
        DishCreateDto dto = new DishCreateDto("NEW_DISH");

        io.restassured.RestAssured.given()
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType("application/json")
                .body(new Gson().toJson(dto))
                .when()
                .post("/dish")
                .then()
                .statusCode(201);

        assertTrue(dishRepository.findByName("NEW_DISH").isPresent());
    }

    @Test
    void testCreateNewDish_unauthorizedWithoutToken() {
        DishCreateDto dto = new DishCreateDto("NEW_DISH_NO_AUTH");

        io.restassured.RestAssured.given()
                .contentType("application/json")
                .body(new Gson().toJson(dto))
                .when()
                .post("/dish")
                .then()
                .statusCode(401); // Unauthorized
    }

    @Test
    void testUpdate() {
        DishUpdateNameDto dto = new DishUpdateNameDto(dishes.get(0).getId(), "UPDATED");

        io.restassured.RestAssured.given()
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType("application/json")
                .body(new Gson().toJson(dto))
                .when()
                .put("/dish")
                .then()
                .statusCode(204);

        assertEquals(
                "UPDATED",
                dishRepository.findById(dishes.get(0).getId()).orElseThrow().getName()
        );
    }

    @Test
    void testFindById() {
        io.restassured.RestAssured.given()
                .header("Authorization", "Bearer " + testJwtToken)
                .queryParam("id", dishes.get(0).getId())
                .when()
                .get("/dish")
                .then()
                .statusCode(200);
    }

    @Test
    void testFindByName() {
        io.restassured.RestAssured.given()
                .header("Authorization", "Bearer " + testJwtToken)
                .queryParam("name", "Dish1")
                .when()
                .get("/dish")
                .then()
                .statusCode(200);
    }

    @Test
    void testDelete() {
        Dish dish = dishes.get(0);

        io.restassured.RestAssured.given()
                .header("Authorization", "Bearer " + testJwtToken)
                .queryParam("id", dish.getId())
                .when()
                .delete("/dish")
                .then()
                .statusCode(204);

        assertFalse(dishRepository.existsById(dish.getId()));
    }

    @Test
    void testAddItem() {
        DishAddItemDto dto = new DishAddItemDto(items.get(0).getId(), dishes.get(0).getId(), 50);

        io.restassured.RestAssured.given()
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType("application/json")
                .body(new Gson().toJson(dto))
                .when()
                .put("/dish/items")
                .then()
                .statusCode(204);
    }

    @Test
    void testUpdateDish_notFound() {
        DishUpdateNameDto dto = new DishUpdateNameDto(99999L, "NON_EXISTENT");

        io.restassured.RestAssured.given()
                .header("Authorization", "Bearer " + testJwtToken)
                .contentType("application/json")
                .body(new Gson().toJson(dto))
                .when()
                .put("/dish")
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteDish_notFound() {
        io.restassured.RestAssured.given()
                .header("Authorization", "Bearer " + testJwtToken)
                .queryParam("id", 99999L)
                .when()
                .delete("/dish")
                .then()
                .statusCode(404);
    }
}