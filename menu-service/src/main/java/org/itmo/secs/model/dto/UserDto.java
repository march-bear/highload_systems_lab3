package org.itmo.secs.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Объект пользователя", description = "Включает имя и ID пользователя, используется при отображении пользователя и обновлении имени")
public record UserDto(
    @Schema(description = "ID пользователя", type = "number", example = "1")
    Long id,
    @Schema(description = "Имя пользователя", type = "string", example = "Олежа")
    String name
) { }
