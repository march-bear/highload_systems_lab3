package org.itmo.secs.domain.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "Объект для манипуляций с блюдом в меню", description = "Содержит ID меню и блюда, используется для добавления и удаления блюда из меню")
public record MenuDishDto(
    @Schema(description = "ID меню", type = "number", example = "1")
    @NotNull Long menuId,
    @Schema(description = "ID блюда", type = "number", example = "2")
    @NotNull Long dishId
) { }
