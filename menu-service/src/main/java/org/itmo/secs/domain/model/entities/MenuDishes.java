package org.itmo.secs.domain.model.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table(name = "menu_dishes")
@Getter
@Setter
@AllArgsConstructor
public class MenuDishes {
    @Id
    Long id;
    @Column("menu_id")
    Long menuId;
    @Column("dish_id")
    Long dishId;
}

