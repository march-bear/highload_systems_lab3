package org.itmo.user.accounter.controllers;

import org.itmo.user.accounter.model.dto.*;
import org.itmo.user.accounter.model.entities.User;
import org.itmo.user.accounter.services.UserService;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Objects;

@AllArgsConstructor
@RestController
@RequestMapping(value = "user")
@Tag(name = "Пользователи (Users API)")
public class UserController {
    private ConversionService conversionService;
    private UserService userService;

    @Operation(summary = "Создать нового пользователя", description = "Создается новый пользователь по отправленному DTO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь был успешно создан",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))
                    }
            ),
            @ApiResponse(responseCode = "400", description = "Пользователь с таким же именем уже есть базе",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                    }
            )
    })
    @PostMapping
    public Mono<ResponseEntity<UserDto>> create(@RequestBody UserCreateDto userDto) {
        User user = new User();
        user.setName(userDto.name());

        return userService.save(user)
                .map(savedUser -> Objects.requireNonNull(conversionService.convert(savedUser, UserDto.class)))
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @Operation(summary = "Изменить пользователя", description = "Изменяет пользователя из БД по отправленному DTO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Успешно изменен"),
            @ApiResponse(responseCode = "400", description = "Пользователь с именем из DTO уже существует",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                    }
            ),
            @ApiResponse(responseCode = "404", description = "Пользователь с id из DTO не был найден",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                    }
            )
    })
    @PutMapping
    public Mono<ResponseEntity<Void>> update(@RequestBody UserDto userDto) {
        User user = new User(userDto.id(), userDto.name());

        return userService.update(user)
                .map(updatedUser -> ResponseEntity.ok().<Void>build())
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Удалить пользователя", description = "Удалить пользователя по id")
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
            return userService.findByName(name)
                    .map(user -> Objects.requireNonNull(conversionService.convert(user, UserDto.class)))
                    .map(dto -> ResponseEntity.ok().body(dto))
                    .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }
    }
}