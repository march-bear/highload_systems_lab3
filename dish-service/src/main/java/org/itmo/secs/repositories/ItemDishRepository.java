package org.itmo.secs.repositories;

import org.itmo.secs.model.entities.ItemDish;

import org.itmo.secs.model.entities.ItemDishId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ItemDishRepository extends JpaRepository<ItemDish, ItemDishId> {
    Optional<ItemDish> findById_ItemIdAndId_DishId(long itemId, long dishId);
    List<ItemDish> findAllById_DishId(long dishId);
}
