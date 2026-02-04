package org.itmo.secs.model.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 3, max = 16)
    @Column(unique = true)
    private String name;

    @NotNull
    @PositiveOrZero
    private Integer calories;

    @NotNull
    @PositiveOrZero
    private Integer carbs;

    @NotNull
    @PositiveOrZero
    private Integer protein;

    @NotNull
    @PositiveOrZero
    private Integer fats;

    @OneToMany(mappedBy = "item", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @NotNull
    private List<ItemDish> items_dishes = new ArrayList<>();
}
