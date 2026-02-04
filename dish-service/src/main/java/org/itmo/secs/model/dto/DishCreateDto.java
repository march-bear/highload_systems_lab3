package org.itmo.secs.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Объект для создания нового блюда", description = "Содержит имя создаваемого блюда")
public record DishCreateDto(
    @Schema(description = "Имя блюда", type = "string", example = "Гречка по-купечески")
    String name
) { }
