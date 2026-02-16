package org.itmo.secs.domain.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "Объект для создания нового блюда", description = "Содержит имя создаваемого блюда")
public record DishCreateDto(
    @Schema(description = "Имя блюда (3-20 символов)", type = "string", example = "Гречка по-купечески")
    @Size(min = 3, max = 30)
    String name
) { }
