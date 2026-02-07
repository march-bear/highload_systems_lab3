package org.itmo.user.accounter.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "Объект для создания пользователя", description = "Содержит имя для нового пользователя")
public record UserAuthDto(
    @Schema(description = "Имя пользователя", type = "string", example = "Олег")
    @Size(min = 3, max = 20, message = "Длина имени пользователя должна быть в пределах от 3 до 20 символов")
    String username,
    @Schema(description = "Пароль", type = "string", example = "password")
    @Size(min = 8, max = 20, message = "Длина пароля должна быть в пределах от 8 до 20 символов")
    String password
) { }
