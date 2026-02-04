package org.itmo.secs.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "items_dishes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemDish {
    @EmbeddedId
    private ItemDishId id = new ItemDishId();

    @ManyToOne
    @JsonIgnore
    @MapsId("itemId")
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne
    @JsonIgnore
    @MapsId("dishId")
    @JoinColumn(name = "dish_id")
    private Dish dish;

    @NotNull
    @PositiveOrZero
    private int count;
}
