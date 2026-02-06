package org.itmo.user.accounter.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.itmo.user.accounter.model.entities.enums.UserRole;

public record UserSetRoleDto(
        @Schema(description = "ID пользователя", type = "number", example = "1")
        Long id,
        @Schema(description = "Роль пользователя", type = "string", example = "USER")
        UserRole role
) {}
