package org.itmo.secs.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(name = "Объект для создания нового продукта", description = "Содержит поля для создания нового продукта")
public record ItemCreateDto(
    @Schema(description = "Имя продукта (от 3 до 30 символов)", type = "string", example = "Хлеб")
    @Size(min = 3, max = 30)
    String name,
    @Schema(description = "Ккал (на 100 г)", type = "number", example = "220")
    @PositiveOrZero
    Integer calories,
    @Schema(description = "Углеводы, г (на 100 г)", type = "number", example = "53")
    @PositiveOrZero
    Integer carbs,
    @Schema(description = "Белки, г (на 100 г)", type = "number", example = "7")
    @PositiveOrZero
    Integer protein,
    @Schema(description = "Жиры, г (на 100 г)", type = "number", example = "1")
    @PositiveOrZero
    Integer fats
) { }
