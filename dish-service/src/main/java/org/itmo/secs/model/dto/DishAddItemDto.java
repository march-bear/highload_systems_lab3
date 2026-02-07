package org.itmo.secs.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(name = "Объект для добавления продукта в состав блюда", description = "Содержит id соответствующих продукта и блюда, а также массу продукта (в граммах)")
public record DishAddItemDto(
    @Schema(description = "ID продукта", type = "number", example = "1")
    Long itemId,
    @Schema(description = "ID блюда", type = "number", example = "2")
    Long dishId,
    @Schema(description = "Граммовка продукта в блюде", type = "number", example = "100")
    @Positive
    Integer count
) { }
