package org.itmo.secs.domain.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Объект блюда", description = "Содержит ID и имя продукта из базы")
public record DishDto(
    @Schema(description = "ID продукта", type = "number", example = "1")
    Long id,
    @Schema(description = "Имя продукта", type = "string", example = "Гречка по-купечески")
    String name,
    @Schema(description = "Ккал", type = "number", example = "220")
    Integer calories,
    @Schema(description = "Углеводы, г", type = "number", example = "53")
    Integer carbs,
    @Schema(description = "Белки, г", type = "number", example = "7")
    Integer protein,
    @Schema(description = "Жиры, г", type = "number", example = "1")
    Integer fats
) { }
