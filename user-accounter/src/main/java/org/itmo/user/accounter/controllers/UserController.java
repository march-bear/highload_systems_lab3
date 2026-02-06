package org.itmo.user.accounter.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.itmo.user.accounter.model.dto.ErrorDto;
import org.itmo.user.accounter.model.dto.UserDto;
import org.itmo.user.accounter.model.dto.UserSetRoleDto;
import org.itmo.user.accounter.services.UserService;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Objects;

@AllArgsConstructor
@RestController
@RequestMapping(value = "/user")
@Tag(name = "Пользователи (Users API)")
public class UserController {
    private ConversionService conversionService;
    private UserService userService;

    @Operation(summary = "Удалить пользователя", description = "Удалить пользователя по id", security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Успшено удален"),
            @ApiResponse(responseCode = "404", description = "Пользователь с отправленным id не был найден",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                    }
            )
    })
    @DeleteMapping
    public Mono<Void> delete(
            @Parameter(description = "ID удаляемого пользователя", example = "1", required = true)
            @RequestParam() long id
    ) {
        return userService.deleteById(id);
    }

    @Operation(summary = "Найти пользователя", description = "При указании id ищет пользователя по id, при указании имени ищет пользователя по имени")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Содержит найденного пользователя",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))
                    }
            ),
            @ApiResponse(responseCode = "400", description = "Ни ID, ни имя для поиска не были указаны"),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным ID или именем не был найдено",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                    }
            )
    })
    @GetMapping
    public Mono<ResponseEntity<UserDto>> find(
            @Parameter(description = "ID пользователя", example = "1")
            @RequestParam(required=false) Long id,
            @Parameter(description = "Имя пользователя", example = "Олежка")
            @RequestParam(required=false) String name
    ) {
        if (id != null) {
            return userService.findById(id)
                    .map(user -> Objects.requireNonNull(conversionService.convert(user, UserDto.class)))
                    .map(dto -> ResponseEntity.ok().body(dto))
                    .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } else if (name != null) {
            return userService.findUserByUsername(name)
                    .map(user -> Objects.requireNonNull(conversionService.convert(user, UserDto.class)))
                    .map(dto -> ResponseEntity.ok().body(dto))
                    .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }
    }

    @GetMapping("/whoami")
    public Mono<ResponseEntity<UserDto>> currentUser() {
        return userService.getCurrentUser()
                .map(user -> conversionService.convert(user, UserDto.class))
                .map(ResponseEntity::ok);
    }

    @PutMapping("/role")
    public Mono<ResponseEntity<UserDto>> setRoleToUser(UserSetRoleDto dto) {
        return userService.updateRole(dto.id(), dto.role())
                .map(user -> conversionService.convert(user, UserDto.class))
                .map(ResponseEntity::ok);
    }
}
