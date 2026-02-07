package org.itmo.user.accounter.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "JWT токен", description = "Действительный токен аутентификации, действителен 5 минут")
public record JwtTokenDto(
        @Schema(description = "Токен", type = "string", example = "some_long_token_value")
        String token
) { }
