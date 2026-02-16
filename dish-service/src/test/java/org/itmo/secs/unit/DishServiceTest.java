package org.itmo.secs.unit;

import org.itmo.secs.domain.model.entities.ItemDish;
import org.itmo.secs.domain.model.entities.Dish;
import org.itmo.secs.domain.model.entities.Item;
import org.itmo.secs.application.repositories.DishRepository;
import org.itmo.secs.application.services.DishService;
import org.itmo.secs.application.services.ItemDishService;
import org.itmo.secs.application.services.ItemService;
import org.itmo.secs.exception.DataIntegrityViolationException;
import org.itmo.secs.exception.ItemNotFoundException;
import org.itmo.secs.infrastructure.notification.DishEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

class DishServiceTest {

    private final ItemDishService itemDishService = mock(ItemDishService.class);
    private final DishRepository dishRepository = mock(DishRepository.class);
    private final ItemService itemService = mock(ItemService.class);
    private final DishEventProducer dishEventProducer = mock(DishEventProducer.class);

    private DishService dishService;

    private Dish dish;
    private Item item;

    @BeforeEach
    void setUp() {
        dishService = new DishService(itemDishService, dishRepository, itemService, dishEventProducer);

        dish = new Dish();
        dish.setId(1L);
        dish.setName("Test");

        item = new Item();
        item.setId(10L);
        item.setName("Test Item");
        item.setCalories(100);
        item.setProtein(10);
        item.setFats(5);
        item.setCarbs(20);

        // Настройка моков для event producer
        doNothing().when(dishEventProducer).sendDishCreated(any());
        doNothing().when(dishEventProducer).sendDishDeleted(any());
        doNothing().when(dishEventProducer).sendDishUpdated(any(), any());
        doNothing().when(dishEventProducer).sendDishUpdatedDish(any(), anyLong(), anyInt());
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
        verify(dishEventProducer).sendDishCreated(dish);
    }

    @Test
    void save_ShouldThrow_WhenNameExists() {
        when(dishRepository.findByName("Test")).thenReturn(java.util.Optional.of(dish));

        StepVerifier.create(dishService.save(dish))
                .expectError(DataIntegrityViolationException.class)
                .verify();

        verify(dishRepository, never()).save(any());
        verify(dishEventProducer, never()).sendDishCreated(any());
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
        verify(dishEventProducer).sendDishUpdatedDish(dish, 10L, 5);
    }

    @Test
    void addItem_ShouldThrow_WhenDishNotFound() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        StepVerifier.create(dishService.addItem(10L, 1L, 5))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(itemDishService, never()).updateItemDishCount(any(), any(), anyInt());
        verify(dishEventProducer, never()).sendDishUpdatedDish(any(), anyLong(), anyInt());
    }

    @Test
    void addItem_ShouldThrow_WhenItemNotFound() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));
        when(itemService.findById(10L)).thenReturn(Mono.empty());

        StepVerifier.create(dishService.addItem(10L, 1L, 5))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(itemDishService, never()).updateItemDishCount(any(), any(), anyInt());
        verify(dishEventProducer, never()).sendDishUpdatedDish(any(), anyLong(), anyInt());
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
        verify(dishEventProducer).sendDishUpdatedDish(dish, 10L, 0);
    }

    @Test
    void deleteItem_ShouldThrow_WhenDishNotFound() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        StepVerifier.create(dishService.deleteItem(10L, 1L))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(itemDishService, never()).delete(any(), any());
        verify(dishEventProducer, never()).sendDishUpdatedDish(any(), anyLong(), anyInt());
    }

    @Test
    void deleteItem_ShouldThrow_WhenItemNotFound() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));
        when(itemService.findById(10L)).thenReturn(Mono.empty());

        StepVerifier.create(dishService.deleteItem(10L, 1L))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(itemDishService, never()).delete(any(), any());
        verify(dishEventProducer, never()).sendDishUpdatedDish(any(), anyLong(), anyInt());
    }

    // ---------- UPDATE NAME ----------

    @Test
    void updateName_ShouldUpdate_WhenValid() {
        Dish updatedDish = new Dish();
        updatedDish.setId(1L);
        updatedDish.setName("Updated Name");

        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));
        when(dishRepository.findByName("Updated Name")).thenReturn(java.util.Optional.empty());
        when(dishRepository.save(updatedDish)).thenReturn(updatedDish);

        StepVerifier.create(dishService.updateName(updatedDish))
                .verifyComplete();

        verify(dishRepository).save(updatedDish);
        verify(dishEventProducer).sendDishUpdated(dish, updatedDish);
    }

    @Test
    void updateName_ShouldWork_WhenNameNotChanged() {
        Dish updatedDish = new Dish();
        updatedDish.setId(1L);
        updatedDish.setName("Test"); // То же имя

        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));
        when(dishRepository.findByName("Test")).thenReturn(java.util.Optional.of(dish));
        when(dishRepository.save(updatedDish)).thenReturn(updatedDish);

        StepVerifier.create(dishService.updateName(updatedDish))
                .verifyComplete();

        verify(dishRepository).save(updatedDish);
        verify(dishEventProducer).sendDishUpdated(dish, updatedDish);
    }

    @Test
    void updateName_ShouldThrow_WhenDishNotFound() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        StepVerifier.create(dishService.updateName(dish))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(dishRepository, never()).save(any());
        verify(dishEventProducer, never()).sendDishUpdated(any(), any());
    }

    @Test
    void updateName_ShouldThrow_WhenNameExistsForDifferentDish() {
        Dish existingDish = new Dish();
        existingDish.setId(2L);
        existingDish.setName("Existing Name");

        Dish updatedDish = new Dish();
        updatedDish.setId(1L);
        updatedDish.setName("Existing Name");

        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));
        when(dishRepository.findByName("Existing Name")).thenReturn(java.util.Optional.of(existingDish));

        StepVerifier.create(dishService.updateName(updatedDish))
                .expectError(DataIntegrityViolationException.class)
                .verify();

        verify(dishRepository, never()).save(any());
        verify(dishEventProducer, never()).sendDishUpdated(any(), any());
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
    void findById_ShouldReturnEmpty_WhenNotFound() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        StepVerifier.create(dishService.findById(1L))
                .verifyComplete();
    }

    @Test
    void findByName_ShouldReturnDish() {
        when(dishRepository.findByName("Test")).thenReturn(java.util.Optional.of(dish));

        StepVerifier.create(dishService.findByName("Test"))
                .expectNext(dish)
                .verifyComplete();
    }

    @Test
    void findByName_ShouldReturnEmpty_WhenNotFound() {
        when(dishRepository.findByName("Non Existent")).thenReturn(java.util.Optional.empty());

        StepVerifier.create(dishService.findByName("Non Existent"))
                .verifyComplete();
    }

    // ---------- DELETE ----------

    @Test
    void delete_ShouldDeleteDish() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));

        StepVerifier.create(dishService.delete(1L))
                .verifyComplete();

        verify(dishRepository).deleteById(1L);
        verify(dishEventProducer).sendDishDeleted(dish);
    }

    @Test
    void delete_ShouldThrow_WhenNotExists() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        StepVerifier.create(dishService.delete(1L))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(dishRepository, never()).deleteById(any());
        verify(dishEventProducer, never()).sendDishDeleted(any());
    }

    // ---------- MAKE LIST ----------

    @Test
    void makeListOfItems_ShouldReturnFlux() {
        ItemDish itemDish = new ItemDish();
        itemDish.setItem(item);
        itemDish.setDish(dish);
        itemDish.setCount(5);

        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));
        when(itemDishService.findAllByDishId(1L)).thenReturn(Flux.just(itemDish));

        StepVerifier.create(dishService.makeListOfItems(1L))
                .expectNext(Pair.of(item, 5))
                .verifyComplete();
    }

    @Test
    void makeListOfItems_ShouldReturnEmpty_WhenNoItems() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.of(dish));
        when(itemDishService.findAllByDishId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(dishService.makeListOfItems(1L))
                .verifyComplete();
    }

    @Test
    void makeListOfItems_ShouldThrow_WhenDishNotFound() {
        when(dishRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        StepVerifier.create(dishService.makeListOfItems(1L))
                .expectError(ItemNotFoundException.class)
                .verify();
    }

    // ---------- FIND ALL ----------

    @Test
    void findAll_ShouldReturnFlux() {
        List<Dish> dishesList = List.of(dish);
        when(dishRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(dishesList));

        StepVerifier.create(dishService.findAll(0, 10))
                .expectNext(dish)
                .verifyComplete();
    }

    @Test
    void findAll_ShouldReturnEmptyFlux() {
        when(dishRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        StepVerifier.create(dishService.findAll(0, 10))
                .verifyComplete();
    }
}