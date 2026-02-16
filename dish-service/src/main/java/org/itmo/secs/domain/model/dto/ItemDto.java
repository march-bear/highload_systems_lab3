package org.itmo.secs.domain.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Объект продукта", description = "Объект продукта, включающий основные характеристики продукта, его имя и ID")
public record ItemDto(
    @Schema(description = "ID", type = "number", example = "47")
    Long id,
    @Schema(description = "Имя продукта (от 3 до 15 символов)", type = "string", example = "Хлеб")
    String name,
    @Schema(description = "Ккал (на 100 г)", type = "number", example = "220")
    Integer calories,
    @Schema(description = "Углеводы, г (на 100 г)", type = "number", example = "53")
    Integer carbs,
    @Schema(description = "Белки, г (на 100 г)", type = "number", example = "7")
    Integer protein,
    @Schema(description = "Жиры, г (на 100 г)", type = "number", example = "1")
    Integer fats
) { }
