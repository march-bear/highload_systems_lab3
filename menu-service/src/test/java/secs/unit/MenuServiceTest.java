package secs.unit;

import org.itmo.secs.infrastructure.client.DishServiceClient;
import org.itmo.secs.infrastructure.client.UserServiceClient;
import org.itmo.secs.domain.model.dto.DishDto;
import org.itmo.secs.domain.model.dto.UserDto;
import org.itmo.secs.domain.model.entities.Menu;
import org.itmo.secs.domain.model.entities.enums.Meal;
import org.itmo.secs.infrastructure.notification.MenuEventProducer;
import org.itmo.secs.application.repositories.MenuRepository;
import org.itmo.secs.application.services.MenuDishesService;
import org.itmo.secs.application.services.MenuService;
import org.itmo.secs.exception.DataIntegrityViolationException;
import org.itmo.secs.exception.ItemNotFoundException;
import org.itmo.secs.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MenuServiceTest {
    private final MenuRepository menuRepository = Mockito.mock(MenuRepository.class);
    private final MenuDishesService menuDishesService = Mockito.mock(MenuDishesService.class);
    private final DishServiceClient dishServiceClient = Mockito.mock(DishServiceClient.class);
    private final UserServiceClient userServiceClient = Mockito.mock(UserServiceClient.class);
    private final MenuEventProducer menuEventProducer = Mockito.mock(MenuEventProducer.class);

    private final MenuService menuService = new MenuService(
            menuRepository,
            menuDishesService,
            dishServiceClient,
            userServiceClient,
            menuEventProducer
    );

    private Menu testMenu;
    private UserDto testUserDto;
    private DishDto testDishDto;
    private LocalDate testDate;
    private String testAuthToken;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 1, 15);
        testAuthToken = "Bearer valid.jwt.token";

        testUserDto = new UserDto(1L, "TestUser");
        testDishDto = new DishDto(100L, "Test Dish", 100, 20, 10, 5);

        testMenu = new Menu();
        testMenu.setId(1L);
        testMenu.setMeal(Meal.BREAKFAST);
        testMenu.setDate(testDate);
        testMenu.setUserId(1L);

        // Настройка моков для event producer
        doNothing().when(menuEventProducer).sendMenuCreated(any());
        doNothing().when(menuEventProducer).sendMenuDeleted(any());
        doNothing().when(menuEventProducer).sendMenuUpdated(any(), any());
        doNothing().when(menuEventProducer).sendMenuUpdatedDish(any(), anyLong(), anyBoolean());
    }

    @Test
    void save_ShouldSaveNewMenu_WhenMenuDoesNotExist() {
        // Arrange
        when(menuRepository.findByMealAndDateAndUserId(any(), any(), any()))
                .thenReturn(Mono.empty());
        when(menuRepository.save(any(Menu.class)))
                .thenReturn(Mono.just(testMenu));

        // Act & Assert
        StepVerifier.create(menuService.save(testMenu))
                .expectNext(testMenu)
                .verifyComplete();

        verify(menuRepository).findByMealAndDateAndUserId(
                testMenu.getMeal(),
                testMenu.getDate(),
                testMenu.getUserId()
        );
        verify(menuRepository).save(testMenu);
        verify(menuEventProducer).sendMenuCreated(testMenu);
    }

    @Test
    void save_ShouldThrowException_WhenMenuAlreadyExists() {
        // Arrange
        when(menuRepository.findByMealAndDateAndUserId(any(), any(), any()))
                .thenReturn(Mono.just(testMenu));

        // Act & Assert
        StepVerifier.create(menuService.save(testMenu))
                .expectError(DataIntegrityViolationException.class)
                .verify();

        verify(menuRepository).findByMealAndDateAndUserId(
                testMenu.getMeal(),
                testMenu.getDate(),
                testMenu.getUserId()
        );
        verify(menuRepository, never()).save(any());
        verify(menuEventProducer, never()).sendMenuCreated(any());
    }

    @Test
    void updateForUser_ShouldUpdateMenu_WhenValidInput() {
        // Arrange
        Menu updatedMenu = new Menu();
        updatedMenu.setId(1L);
        updatedMenu.setMeal(Meal.LUNCH);
        updatedMenu.setDate(testDate.plusDays(1));
        updatedMenu.setUserId(1L);

        when(menuRepository.findById(1L)).thenReturn(Mono.just(testMenu));
        when(menuRepository.findByMealAndDateAndUserId(any(), any(), any()))
                .thenReturn(Mono.empty());
        when(menuRepository.save(any(Menu.class)))
                .thenReturn(Mono.just(updatedMenu));

        // Act & Assert
        StepVerifier.create(menuService.updateForUser(updatedMenu, 1L))
                .verifyComplete();

        verify(menuRepository).save(argThat(menu ->
                menu.getMeal() == Meal.LUNCH &&
                        menu.getDate().equals(testDate.plusDays(1))
        ));
        verify(menuEventProducer).sendMenuUpdated(eq(testMenu), any(Menu.class));
    }

    @Test
    void updateForUser_ShouldThrowException_WhenMenuNotFound() {
        // Arrange
        when(menuRepository.findById(1L)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(menuService.updateForUser(testMenu, 1L))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(menuRepository, never()).save(any());
        verify(menuEventProducer, never()).sendMenuUpdated(any(), any());
    }

    @Test
    void updateForUser_ShouldThrowException_WhenUserIdDoesNotMatch() {
        // Arrange
        when(menuRepository.findById(1L)).thenReturn(Mono.just(testMenu));

        // Act & Assert - пытаемся обновить меню другого пользователя
        StepVerifier.create(menuService.updateForUser(testMenu, 999L))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(menuRepository, never()).save(any());
        verify(menuEventProducer, never()).sendMenuUpdated(any(), any());
    }

    @Test
    void updateForUser_ShouldThrowException_WhenNewKeyAlreadyExists() {
        // Arrange
        Menu existingMenuWithNewKey = new Menu();
        existingMenuWithNewKey.setId(2L); // Другое ID
        existingMenuWithNewKey.setMeal(Meal.LUNCH);
        existingMenuWithNewKey.setDate(testDate);
        existingMenuWithNewKey.setUserId(1L);

        Menu updatedMenu = new Menu();
        updatedMenu.setId(1L);
        updatedMenu.setMeal(Meal.LUNCH);
        updatedMenu.setDate(testDate);
        updatedMenu.setUserId(1L);

        when(menuRepository.findById(1L)).thenReturn(Mono.just(testMenu));
        when(menuRepository.findByMealAndDateAndUserId(Meal.LUNCH, testDate, 1L))
                .thenReturn(Mono.just(existingMenuWithNewKey));

        // Act & Assert
        StepVerifier.create(menuService.updateForUser(updatedMenu, 1L))
                .expectError(DataIntegrityViolationException.class)
                .verify();

        verify(menuRepository, never()).save(any());
        verify(menuEventProducer, never()).sendMenuUpdated(any(), any());
    }

    @Test
    void delete_ShouldDeleteMenu_WhenMenuExists() {
        // Arrange
        when(menuRepository.findById(1L)).thenReturn(Mono.just(testMenu));
        when(menuRepository.deleteById(1L)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(menuService.delete(1L))
                .verifyComplete();

        verify(menuRepository).deleteById(1L);
        verify(menuEventProducer).sendMenuDeleted(testMenu);
    }

    @Test
    void delete_ShouldThrowException_WhenMenuNotFound() {
        // Arrange
        when(menuRepository.findById(1L)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(menuService.delete(1L))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(menuRepository, never()).deleteById(anyLong());
        verify(menuEventProducer, never()).sendMenuDeleted(any());
    }

    @Test
    void deleteForUser_ShouldDeleteMenu_WhenUserIsOwner() {
        // Arrange
        when(menuRepository.findById(1L)).thenReturn(Mono.just(testMenu));
        when(menuRepository.deleteById(1L)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(menuService.deleteForUser(1L, 1L))
                .verifyComplete();

        verify(menuRepository).deleteById(1L);
        verify(menuEventProducer).sendMenuDeleted(testMenu);
    }

    @Test
    void deleteForUser_ShouldThrowException_WhenUserIsNotOwner() {
        // Arrange
        when(menuRepository.findById(1L)).thenReturn(Mono.just(testMenu));

        // Act & Assert
        StepVerifier.create(menuService.deleteForUser(1L, 999L))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(menuRepository, never()).deleteById(anyLong());
        verify(menuEventProducer, never()).sendMenuDeleted(any());
    }

    @Test
    void findById_ShouldReturnMenu_WhenExists() {
        // Arrange
        when(menuRepository.findById(1L)).thenReturn(Mono.just(testMenu));

        // Act & Assert
        StepVerifier.create(menuService.findById(1L))
                .expectNext(testMenu)
                .verifyComplete();
    }

    @Test
    void findById_ShouldThrowException_WhenNotFound() {
        // Arrange
        when(menuRepository.findById(1L)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(menuService.findById(1L))
                .expectError(ItemNotFoundException.class)
                .verify();
    }

    @Test
    void findByKey_ShouldReturnMenu_WhenExists() {
        // Arrange
        when(menuRepository.findByMealAndDateAndUserId(
                Meal.BREAKFAST, testDate, 1L))
                .thenReturn(Mono.just(testMenu));

        // Act & Assert
        StepVerifier.create(menuService.findByKey(Meal.BREAKFAST, testDate, 1L))
                .expectNext(testMenu)
                .verifyComplete();
    }

    @Test
    void findByKey_ShouldReturnEmpty_WhenNotExists() {
        // Arrange
        when(menuRepository.findByMealAndDateAndUserId(
                Meal.BREAKFAST, testDate, 1L))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(menuService.findByKey(Meal.BREAKFAST, testDate, 1L))
                .verifyComplete();
    }

    @Test
    void includeDishToMenuForUser_ShouldAddDish_WhenValidInput() {
        // Arrange
        Long menuId = 1L;
        Long dishId = 100L;
        Long userId = 1L;

        when(menuRepository.findById(menuId)).thenReturn(Mono.just(testMenu));
        when(menuDishesService.getDishesIdByMenuId(menuId))
                .thenReturn(Flux.just(50L, 60L)); // Существующие блюда
        when(dishServiceClient.getById(dishId))
                .thenReturn(Mono.just(testDishDto));
        when(menuDishesService.saveByIds(menuId, dishId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(menuService.includeDishToMenuForUser(dishId, menuId, userId))
                .verifyComplete();

        verify(menuDishesService).saveByIds(menuId, dishId);
        verify(menuEventProducer).sendMenuUpdatedDish(eq(testMenu), eq(dishId), eq(false));
    }

    @Test
    void includeDishToMenuForUser_ShouldThrowException_WhenMenuNotFound() {
        // Arrange
        Long menuId = 1L;
        Long dishId = 100L;
        Long userId = 1L;

        when(menuRepository.findById(menuId)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(menuService.includeDishToMenuForUser(dishId, menuId, userId))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(menuDishesService, never()).saveByIds(any(), any());
        verify(menuEventProducer, never()).sendMenuUpdatedDish(any(), anyLong(), anyBoolean());
    }

    @Test
    void includeDishToMenuForUser_ShouldThrowException_WhenUserIsNotOwner() {
        // Arrange
        Long menuId = 1L;
        Long dishId = 100L;
        Long userId = 999L;

        when(menuRepository.findById(menuId)).thenReturn(Mono.just(testMenu));

        // Act & Assert
        StepVerifier.create(menuService.includeDishToMenuForUser(dishId, menuId, userId))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(menuDishesService, never()).saveByIds(any(), any());
        verify(menuEventProducer, never()).sendMenuUpdatedDish(any(), anyLong(), anyBoolean());
    }

    @Test
    void includeDishToMenuForUser_ShouldThrowException_WhenDishAlreadyInMenu() {
        // Arrange
        Long menuId = 1L;
        Long dishId = 100L;
        Long userId = 1L;

        when(menuRepository.findById(menuId)).thenReturn(Mono.just(testMenu));
        when(menuDishesService.getDishesIdByMenuId(menuId))
                .thenReturn(Flux.just(50L, 100L, 60L)); // Блюдо уже есть

        // Act & Assert
        StepVerifier.create(menuService.includeDishToMenuForUser(dishId, menuId, userId))
                .expectError(DataIntegrityViolationException.class)
                .verify();

        verify(menuDishesService, never()).saveByIds(any(), any());
        verify(dishServiceClient, never()).getById(any());
        verify(menuEventProducer, never()).sendMenuUpdatedDish(any(), anyLong(), anyBoolean());
    }

    @Test
    void includeDishToMenuForUser_ShouldThrowException_WhenDishNotFound() {
        // Arrange
        Long menuId = 1L;
        Long dishId = 100L;
        Long userId = 1L;

        when(menuRepository.findById(menuId)).thenReturn(Mono.just(testMenu));
        when(menuDishesService.getDishesIdByMenuId(menuId))
                .thenReturn(Flux.just(50L, 60L));
        when(dishServiceClient.getById(dishId))
                .thenReturn(Mono.error(new ItemNotFoundException("Dish not found")));

        // Act & Assert
        StepVerifier.create(menuService.includeDishToMenuForUser(dishId, menuId, userId))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(menuDishesService, never()).saveByIds(any(), any());
        verify(menuEventProducer, never()).sendMenuUpdatedDish(any(), anyLong(), anyBoolean());
    }

    @Test
    void includeDishToMenuForUser_ShouldPropagateServiceUnavailableException() {
        // Arrange
        Long menuId = 1L;
        Long dishId = 100L;
        Long userId = 1L;

        when(menuRepository.findById(menuId)).thenReturn(Mono.just(testMenu));
        when(menuDishesService.getDishesIdByMenuId(menuId))
                .thenReturn(Flux.just(50L, 60L));
        when(dishServiceClient.getById(dishId))
                .thenReturn(Mono.error(new ServiceUnavailableException("Dish service unavailable")));

        // Act & Assert
        StepVerifier.create(menuService.includeDishToMenuForUser(dishId, menuId, userId))
                .expectError(ServiceUnavailableException.class)
                .verify();

        verify(menuDishesService, never()).saveByIds(any(), any());
        verify(menuEventProducer, never()).sendMenuUpdatedDish(any(), anyLong(), anyBoolean());
    }

    @Test
    void deleteDishFromMenuForUser_ShouldRemoveDish_WhenExists() {
        // Arrange
        Long menuId = 1L;
        Long dishId = 100L;
        Long userId = 1L;

        when(menuRepository.findById(menuId)).thenReturn(Mono.just(testMenu));
        when(menuDishesService.getDishesIdByMenuId(menuId))
                .thenReturn(Flux.just(50L, 100L, 60L)); // Блюдо есть в меню
        when(menuDishesService.deleteByIds(menuId, dishId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(menuService.deleteDishFromMenuForUser(dishId, menuId, userId))
                .verifyComplete();

        verify(menuDishesService).deleteByIds(menuId, dishId);
        verify(menuEventProducer).sendMenuUpdatedDish(eq(testMenu), eq(dishId), eq(true));
    }

    @Test
    void deleteDishFromMenuForUser_ShouldThrowException_WhenDishNotInMenu() {
        // Arrange
        Long menuId = 1L;
        Long dishId = 100L;
        Long userId = 1L;

        when(menuRepository.findById(menuId)).thenReturn(Mono.just(testMenu));
        when(menuDishesService.getDishesIdByMenuId(menuId))
                .thenReturn(Flux.just(50L, 60L)); // Блюда нет в меню

        // Act & Assert
        StepVerifier.create(menuService.deleteDishFromMenuForUser(dishId, menuId, userId))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(menuDishesService, never()).deleteByIds(any(), any());
        verify(menuEventProducer, never()).sendMenuUpdatedDish(any(), anyLong(), anyBoolean());
    }

    @Test
    void makeListOfDishesForUser_ShouldReturnDishes_WhenValidInput() {
        // Arrange
        Long menuId = 1L;
        Long userId = 1L;
        List<Long> dishIds = Arrays.asList(100L, 200L);

        when(menuRepository.findById(menuId)).thenReturn(Mono.just(testMenu));
        when(menuDishesService.getDishesIdByMenuId(menuId))
                .thenReturn(Flux.fromIterable(dishIds));
        when(dishServiceClient.getById(100L))
                .thenReturn(Mono.just(testDishDto));
        when(dishServiceClient.getById(200L))
                .thenReturn(Mono.just(new DishDto(200L, "Another Dish", 150, 30, 15, 8)));

        // Act & Assert
        StepVerifier.create(menuService.makeListOfDishesForUser(menuId, userId))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void makeListOfDishesForUser_ShouldReturnNotFoundDish_WhenDishServiceFails() {
        // Arrange
        Long menuId = 1L;
        Long userId = 1L;

        when(menuRepository.findById(menuId)).thenReturn(Mono.just(testMenu));
        when(menuDishesService.getDishesIdByMenuId(menuId))
                .thenReturn(Flux.just(100L));
        when(dishServiceClient.getById(100L))
                .thenReturn(Mono.error(new RuntimeException("Some error")));

        // Act & Assert
        StepVerifier.create(menuService.makeListOfDishesForUser(menuId, userId))
                .expectNextMatches(dish ->
                        dish.id().equals(100L) &&
                                dish.name().equals("(not found)")
                )
                .verifyComplete();
    }

    @Test
    void makeListOfDishesForUser_ShouldThrowException_WhenUserIsNotOwner() {
        // Arrange
        Long menuId = 1L;
        Long userId = 999L;

        when(menuRepository.findById(menuId)).thenReturn(Mono.just(testMenu));

        // Act & Assert
        StepVerifier.create(menuService.makeListOfDishesForUser(menuId, userId))
                .expectError(ItemNotFoundException.class)
                .verify();
    }

    @Test
    void findAllByUserId_ShouldReturnUserMenus_WithPagination() {
        // Arrange
        int page = 0;
        int size = 10;
        Long userId = 1L;

        Menu menu1 = new Menu();
        menu1.setId(1L);
        menu1.setUserId(1L);

        Menu menu2 = new Menu();
        menu2.setId(2L);
        menu2.setUserId(1L);

        Menu otherUserMenu = new Menu();
        otherUserMenu.setId(3L);
        otherUserMenu.setUserId(2L);

        when(menuRepository.findAll())
                .thenReturn(Flux.just(menu1, menu2, otherUserMenu));

        // Act & Assert
        StepVerifier.create(menuService.findAllByUserId(page, size, userId))
                .expectNextCount(2) // Только меню пользователя 1L
                .verifyComplete();
    }

    @Test
    void findAllByUserId_ShouldReturnEmpty_WhenNoMenusForUser() {
        // Arrange
        int page = 0;
        int size = 10;
        Long userId = 999L;

        Menu menu1 = new Menu();
        menu1.setId(1L);
        menu1.setUserId(1L);

        Menu menu2 = new Menu();
        menu2.setId(2L);
        menu2.setUserId(2L);

        when(menuRepository.findAll())
                .thenReturn(Flux.just(menu1, menu2));

        // Act & Assert
        StepVerifier.create(menuService.findAllByUserId(page, size, userId))
                .verifyComplete();
    }

    @Test
    void findAll_ShouldReturnAllMenus_WithPagination() {
        // Arrange
        int page = 0;
        int size = 2;

        Menu menu1 = new Menu();
        menu1.setId(1L);

        Menu menu2 = new Menu();
        menu2.setId(2L);

        Menu menu3 = new Menu();
        menu3.setId(3L);

        when(menuRepository.findAll())
                .thenReturn(Flux.just(menu1, menu2, menu3));

        // Act & Assert
        StepVerifier.create(menuService.findAll(page, size))
                .expectNextCount(2) // Только первые 2 из-за пагинации
                .verifyComplete();
    }

    @Test
    void findAll_ShouldApplyPaginationCorrectly() {
        // Arrange
        int page = 1;
        int size = 1;

        Menu menu1 = new Menu();
        menu1.setId(1L);

        Menu menu2 = new Menu();
        menu2.setId(2L);

        Menu menu3 = new Menu();
        menu3.setId(3L);

        when(menuRepository.findAll())
                .thenReturn(Flux.just(menu1, menu2, menu3));

        // Act & Assert
        StepVerifier.create(menuService.findAll(page, size))
                .expectNextCount(1) // Только 2-й элемент (индекс 1)
                .verifyComplete();
    }

    @Test
    void findAllByUsername_ShouldReturnUserMenus_WhenUserExists() {
        // Arrange
        String username = "TestUser";

        Menu menu1 = new Menu();
        menu1.setId(1L);
        menu1.setUserId(1L);

        Menu menu2 = new Menu();
        menu2.setId(2L);
        menu2.setUserId(1L);

        when(userServiceClient.getByName(testAuthToken, username))
                .thenReturn(Mono.just(testUserDto));
        when(menuRepository.findAllByUserId(1L))
                .thenReturn(Flux.just(menu1, menu2));

        // Act & Assert
        StepVerifier.create(menuService.findAllByUsername(testAuthToken, username))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findAllByUsername_ShouldPropagateError_WhenUserServiceFails() {
        // Arrange
        String username = "NonExistentUser";

        when(userServiceClient.getByName(testAuthToken, username))
                .thenReturn(Mono.error(new RuntimeException("User not found")));

        // Act & Assert
        StepVerifier.create(menuService.findAllByUsername(testAuthToken, username))
                .expectError(RuntimeException.class)
                .verify();

        verify(menuRepository, never()).findAllByUserId(any());
    }

    @Test
    void findAllByUsername_ShouldReturnEmpty_WhenUserHasNoMenus() {
        // Arrange
        String username = "TestUser";

        when(userServiceClient.getByName(testAuthToken, username))
                .thenReturn(Mono.just(testUserDto));
        when(menuRepository.findAllByUserId(1L))
                .thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(menuService.findAllByUsername(testAuthToken, username))
                .verifyComplete();
    }
}