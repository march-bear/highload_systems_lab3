package org.itmo.secs.domain.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "Объект для обновления блюда", description = "Позволяет изменить имя продукта по ID")
public record DishUpdateNameDto(
    @Schema(description = "ID продукта", type = "number", example = "1")
    Long id,
    @Schema(description = "Имя продукта (3-20 симв)", type = "string", example = "Гречка по-крестьянски")
    @Size(min = 3, max = 30)
    String name
) { }
