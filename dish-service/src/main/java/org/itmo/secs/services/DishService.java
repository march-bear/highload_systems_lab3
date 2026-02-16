package org.itmo.secs.services;

import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.itmo.secs.notification.DishEventProducer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;

import org.itmo.secs.model.entities.Dish;
import org.itmo.secs.model.entities.Item;
import org.itmo.secs.repositories.DishRepository;
import org.itmo.secs.utils.exceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
public class DishService {
    private final ItemDishService itemDishService;
    private final DishRepository dishRepository;
    private final ItemService itemService;
    private final DishEventProducer dishEventProducer;

    @Transactional
    public Mono<Dish> save(Dish dish) {
        if (dishRepository.findByName(dish.getName()).isPresent()) {
             return Mono.error(new DataIntegrityViolationException("Dish with name " + dish.getName() + " already exist"));
        }

        return Mono.just(dishRepository.save(dish)).doOnSuccess(dishEventProducer::sendDishCreated);
    }

    @Transactional
    public Mono<Void> addItem(Long itemId, Long dishId, int count) {
        Dish dish = dishRepository.findById(dishId).orElse(null);
        if (dish == null) {
            return Mono.error(new ItemNotFoundException("Dish with id " + dishId + " was not found"));
        }

        return itemService.findById(itemId)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Item with id " + itemId + " was not found")))
                .flatMap(item -> itemDishService.updateItemDishCount(item, dish, count))
                .doOnSuccess(itemDish -> dishEventProducer.sendDishUpdatedDish(dish, itemId, count))
                .then();
    }

    @Transactional
    public Mono<Void> deleteItem(Long itemId, Long dishId) {
        Dish dish = dishRepository.findById(dishId).orElse(null);

        if (dish == null) {
            return Mono.error( new ItemNotFoundException("Dish with id " + dishId + " was not found"));
        }

        return itemService.findById(itemId)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Item with id " + itemId + " was not found")))
                .doOnSuccess(itemDish -> dishEventProducer.sendDishUpdatedDish(dish, itemId, 0))
                .flatMap(item -> itemDishService.delete(item, dish)).then();
    }

    @Transactional
    public Mono<Void> updateName(Dish dish) {
        return findById(dish.getId())
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Dish with id " + dish.getId() + " was not found")))
                .flatMap(orig -> findByName(dish.getName())
                        .flatMap(x -> {
                            if (!Objects.equals(x.getId(), dish.getId())) {
                                return Mono.error(new DataIntegrityViolationException("Dish with name " + dish.getName() + " already exist"));
                            } else {
                                return Mono.just(orig);
                            }
                        })
                        .switchIfEmpty(Mono.just(orig))
                        .doOnSuccess(old -> dishEventProducer.sendDishUpdated(old, dish))
                )
                .map(x -> dishRepository.save(dish)).then();
    }

    public Mono<Dish> findById(Long id) {
        Dish dish = dishRepository.findById(id).orElse(null);
        return (dish != null) ? Mono.just(dish) : Mono.empty();
    }

    public Mono<Dish> findByName(String name) {
        Dish dish = dishRepository.findByName(name).orElse(null);
        return (dish != null) ? Mono.just(dish) : Mono.empty();
    }

    @Transactional
    public Mono<Void> delete(Long id) {
        Dish dish = dishRepository.findById(id).orElse(null);
        if (dish == null) {
            return Mono.error(new ItemNotFoundException("Dish with id " + id + " was not found"));
        }
        dishEventProducer.sendDishDeleted(dish);
        dishRepository.deleteById(id);
        return Mono.empty();
    }

    @Transactional
    public Flux<Pair<Item, Integer>> makeListOfItems(Long dishId) {
        return findById(dishId)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Dish with id " + dishId + " was not found")))
                .flatMapMany(it -> itemDishService.findAllByDishId(dishId))
                .map(itemDish -> Pair.of(itemDish.getItem(), itemDish.getCount()));
    }

    public Flux<Dish> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return Flux.fromIterable(dishRepository.findAll(pageable).toList());
    }
}
