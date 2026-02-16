package org.itmo.secs.domain.model.entities;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.itmo.secs.domain.model.entities.enums.Meal;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@Table(name = "menus")
@Getter
@Setter
public class Menu {
    @Id
    private Long id;
    private LocalDate date;
    private Long userId;
    private Meal meal;
}
