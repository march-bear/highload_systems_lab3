package org.itmo.secs.services;

import lombok.AllArgsConstructor;
import org.itmo.secs.model.entities.Dish;
import org.itmo.secs.model.entities.Item;
import org.itmo.secs.model.entities.ItemDish;
import org.itmo.secs.repositories.ItemDishRepository;
import org.itmo.secs.utils.exceptions.ItemNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Service
@AllArgsConstructor
public class ItemDishService {
    private final ItemDishRepository itemDishRepository;

    @Transactional
    public Mono<ItemDish> updateItemDishCount(Item item, Dish dish, int count) {
        Optional<ItemDish> itemDishOpt = itemDishRepository.findById_ItemIdAndId_DishId(item.getId(), dish.getId());
        ItemDish persistUnit = new ItemDish();
        if (itemDishOpt.isEmpty()) {
            persistUnit.setItem(item);
            persistUnit.setDish(dish);
            persistUnit.setCount(count);
        } else {
            persistUnit = itemDishOpt.get();
            persistUnit.setCount(count);
        }
        return Mono.just(itemDishRepository.save(persistUnit));
    }

    @Transactional
    public Mono<ItemDish> delete(Item item, Dish dish) {
        return findById(item.getId(), dish.getId())
            .switchIfEmpty(Mono.error(new ItemNotFoundException("Item with id " + item.getId() + " was not found in Dish with id " + dish.getId())))
            .map(x -> {
                itemDishRepository.delete(x);
                return x;
            });
    }

    public Mono<ItemDish> findById(long itemId, long dishId) {
        ItemDish itemDish = itemDishRepository.findById_ItemIdAndId_DishId(itemId, dishId).orElse(null);
        return (itemDish != null) ? Mono.just(itemDish) : Mono.empty();
    }

    public Flux<ItemDish> findAllByDishId(long dishId) {
        return Flux.fromIterable(itemDishRepository.findAllById_DishId(dishId));
    }
}
