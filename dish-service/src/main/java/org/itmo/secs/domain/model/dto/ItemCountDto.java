package org.itmo.secs.domain.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Пара из продукта и его граммовки в блюде", description = "Используется при получении списка продуктов в блюде")
public record ItemCountDto(
    @Schema(description = "Объект продукта", type = "object")
    ItemDto item,
    @Schema(description = "Граммовка в блюде", type = "number", example = "100")
    int count
) {}
