package org.itmo.secs.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Объект для обновления блюда", description = "Позволяет изменить имя продукта по ID")
public record DishUpdateNameDto(
    @Schema(description = "ID продукта", type = "number", example = "1")
    Long id,
    @Schema(description = "Имя продукта", type = "string", example = "Гречка по-крестьянски")
    String name
) { }
