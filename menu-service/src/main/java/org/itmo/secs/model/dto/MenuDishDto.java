package org.itmo.secs.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Объект для манипуляций с блюдом в меню", description = "Содержит ID меню и блюда, используется для добавления и удаления блюда из меню")
public record MenuDishDto(
    @Schema(description = "ID меню", type = "number", example = "1")
    Long menuId,
    @Schema(description = "ID блюда", type = "number", example = "2")
    Long dishId
) { }
