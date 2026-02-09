package org.itmo.secs.unit;

import org.itmo.secs.model.entities.Dish;
import org.itmo.secs.model.entities.Item;
import org.itmo.secs.repositories.DishRepository;
import org.itmo.secs.services.DishService;
import org.itmo.secs.services.ItemDishService;
import org.itmo.secs.services.ItemService;
import org.itmo.secs.utils.exceptions.DataIntegrityViolationException;
import org.itmo.secs.utils.exceptions.ItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

class DishServiceTest {

    private final ItemDishService itemDishService = mock(ItemDishService.class);
    private final DishRepository dishRepository = mock(DishRepository.class);
    private final ItemService itemService = mock(ItemService.class);

    private DishService dishService;

    private Dish dish;
    private Item item;

    @BeforeEach
    void setUp() {
        dishService = new DishService(itemDishService, dishRepository, itemService);

        dish = new Dish();
        dish.setId(1L);
        dish.setName("Test");

        item = new Item();
        item.setId(10L);
    }

    // ---------- SAVE ----------

    @Test
    void save_ShouldSaveDish_WhenNotExists() {
        when(dishRepository.findByName("Test")).thenReturn(java.util.Optional.empty());
        when(dishRepository.save(dish)).thenReturn(dish);

        StepVerifier.create(dishService.save(dish))
                .expectNext(dish)
                .verifyComplete();

        verify(dishRepository).save(dish);
    }

    @Test
    void save_ShouldThrow_WhenNameExists() {
        when(dishRepository.findByName("Test")).thenReturn(java.util.Optional.of(dish));

        StepVerifier.create(dishService.save(dish))
                .expectError(DataIntegrityViolationException.class)
                .verify();

        verify(dishRepository, never()).save(any());
    }

    // ---------- ADD ITEM ----------

    @Test
    void addItem_ShouldWork() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));
        when(itemService.findById(10L)).thenReturn(Mono.just(item));
        when(itemDishService.updateItemDishCount(item, dish, 5))
                .thenReturn(Mono.empty());

        StepVerifier.create(dishService.addItem(10L, 1L, 5))
                .verifyComplete();

        verify(itemDishService).updateItemDishCount(item, dish, 5);
    }

    @Test
    void addItem_ShouldThrow_WhenDishNotFound() {
        Long dishId = 1L;
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        StepVerifier.create(dishService.addItem(10L, 1L, 5))
                .expectErrorMatches(throwable ->
                        throwable instanceof ItemNotFoundException
                ).verify();
    }

    // ---------- DELETE ITEM ----------

    @Test
    void deleteItem_ShouldWork() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));
        when(itemService.findById(10L)).thenReturn(Mono.just(item));
        when(itemDishService.delete(item, dish)).thenReturn(Mono.empty());

        StepVerifier.create(dishService.deleteItem(10L, 1L))
                .verifyComplete();

        verify(itemDishService).delete(item, dish);
    }

    // ---------- FIND ----------

    @Test
    void findById_ShouldReturnDish() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));

        StepVerifier.create(dishService.findById(1L))
                .expectNext(dish)
                .verifyComplete();
    }

    @Test
    void findByName_ShouldReturnDish() {
        when(dishRepository.findByName("Test")).thenReturn(java.util.Optional.of(dish));

        StepVerifier.create(dishService.findByName("Test"))
                .expectNext(dish)
                .verifyComplete();
    }

    // ---------- DELETE ----------

    @Test
    void delete_ShouldDeleteDish() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));

        StepVerifier.create(dishService.delete(1L))
                .verifyComplete();

        verify(dishRepository).deleteById(1L);
    }

    @Test
    void delete_ShouldThrow_WhenNotExists() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        StepVerifier.create(dishService.delete(1L))
                .expectError(ItemNotFoundException.class)
                .verify();
    }

    // ---------- MAKE LIST ----------

    @Test
    void makeList_ShouldReturnFlux() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));
        when(itemDishService.findAllByDishId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(dishService.makeListOfItems(1L))
                .verifyComplete();
    }

    // ---------- FIND ALL ----------

    @Test
    void findAll_ShouldReturnFlux() {
        when(dishRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dish)));

        StepVerifier.create(dishService.findAll(0, 10))
                .expectNext(dish)
                .verifyComplete();
    }
}
