package org.itmo.secs.unit;

import org.itmo.secs.model.entities.Item;
import org.itmo.secs.repositories.ItemRepository;
import org.itmo.secs.services.ItemService;
import org.itmo.secs.utils.exceptions.DataIntegrityViolationException;
import org.itmo.secs.utils.exceptions.ItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class ItemServiceTest {

    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final ItemService itemService = new ItemService(itemRepository);

    private Item item;

    @BeforeEach
    void setUp() {
        item = new Item();
        item.setId(1L);
        item.setName("Test Item");
        item.setCalories(300);
        item.setProtein(20);
        item.setFats(10);
        item.setCarbs(50);
    }

    // ---------- SAVE ----------

    @Test
    void save_ShouldSaveItem_WhenNotExists() {
        when(itemRepository.findByName("Test Item")).thenReturn(Optional.empty());
        when(itemRepository.save(item)).thenReturn(item);

        StepVerifier.create(itemService.save(item))
                .expectNext(item)
                .verifyComplete();

        verify(itemRepository).save(item);
    }

    @Test
    void save_ShouldThrow_WhenNameExists() {
        when(itemRepository.findByName("Test Item")).thenReturn(Optional.of(item));

        StepVerifier.create(itemService.save(item))
                .expectError(DataIntegrityViolationException.class)
                .verify();

        verify(itemRepository, never()).save(any());
    }

    // ---------- UPDATE ----------

    @Test
    void update_ShouldUpdateItem_WhenValid() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.findByName("Updated Name")).thenReturn(Optional.empty());
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Item updatedItem = new Item();
        updatedItem.setId(1L);
        updatedItem.setName("Updated Name");
        updatedItem.setCalories(400);
        updatedItem.setProtein(25);
        updatedItem.setFats(15);
        updatedItem.setCarbs(60);

        StepVerifier.create(itemService.update(updatedItem))
                .verifyComplete();

        verify(itemRepository).save(any(Item.class));}

    @Test
    void update_ShouldThrow_WhenItemNotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        StepVerifier.create(itemService.update(item))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(itemRepository, never()).save(any());
    }

    @Test
    void update_ShouldThrow_WhenNameExistsForDifferentItem() {
        Item existingItem = new Item();
        existingItem.setId(2L);
        existingItem.setName("Existing Name");

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.findByName("Existing Name")).thenReturn(Optional.of(existingItem));

        Item updatedItem = new Item();
        updatedItem.setId(1L);
        updatedItem.setName("Existing Name");

        StepVerifier.create(itemService.update(updatedItem))
                .expectError(DataIntegrityViolationException.class)
                .verify();

        verify(itemRepository, never()).save(any());
    }

    // ---------- FIND ----------

    @Test
    void findById_ShouldReturnItem() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        StepVerifier.create(itemService.findById(1L))
                .expectNext(item)
                .verifyComplete();
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        StepVerifier.create(itemService.findById(1L))
                .verifyComplete();
    }

    @Test
    void findByName_ShouldReturnItem() {
        when(itemRepository.findByName("Test Item")).thenReturn(Optional.of(item));

        StepVerifier.create(itemService.findByName("Test Item"))
                .expectNext(item)
                .verifyComplete();
    }

    @Test
    void findByName_ShouldReturnEmpty_WhenNotFound() {
        when(itemRepository.findByName("Non Existent")).thenReturn(Optional.empty());

        StepVerifier.create(itemService.findByName("Non Existent"))
                .verifyComplete();
    }

    // ---------- DELETE ----------

    @Test
    void delete_ShouldDeleteItem() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        StepVerifier.create(itemService.delete(1L))
                .verifyComplete();

        verify(itemRepository).deleteById(1L);
    }

    @Test
    void delete_ShouldThrow_WhenNotExists() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        StepVerifier.create(itemService.delete(1L))
                .expectError(ItemNotFoundException.class)
                .verify();

        verify(itemRepository, never()).deleteById(any());
    }

    // ---------- FIND ALL ----------

    @Test
    void findAll_ShouldReturnFlux() {
        List<Item> itemsList = List.of(item);
        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(itemsList));

        StepVerifier.create(itemService.findAll(0, 10))
                .expectNext(item)
                .verifyComplete();
    }

    @Test
    void findAll_ShouldReturnEmptyFlux() {
        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        StepVerifier.create(itemService.findAll(0, 10))
                .verifyComplete();
    }

    // ---------- COUNT ----------

    @Test
    void count_ShouldReturnCount() {
        when(itemRepository.count()).thenReturn(5L);

        StepVerifier.create(itemService.count())
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    void count_ShouldReturnZero() {
        when(itemRepository.count()).thenReturn(0L);

        StepVerifier.create(itemService.count())
                .expectNext(0L)
                .verifyComplete();
    }
}