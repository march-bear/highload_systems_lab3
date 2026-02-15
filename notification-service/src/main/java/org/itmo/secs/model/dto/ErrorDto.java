package org.itmo.secs.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Объект ошибки", description = "Возвращается с ответами 4xx, содержит поясняющее описание ошибки")
public record ErrorDto(
    @Schema(description = "Описание ошибки", type = "string", example = "Some error message")
    String message
) { }
