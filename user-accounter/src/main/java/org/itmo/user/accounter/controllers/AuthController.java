package org.itmo.user.accounter.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.itmo.user.accounter.model.dto.*;
import org.itmo.user.accounter.services.AuthenticationService;
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


@AllArgsConstructor
@RestController
@RequestMapping(value = "/")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Аутентификация (Auth API)")
public class AuthController {
    private AuthenticationService authService;
    private ConversionService conversionService;

    @Operation(summary = "Аутентификация", description = "Производится аутентификация пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аутентификация прошла успешно",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = JwtTokenDto.class))
                    }
            ),
            @ApiResponse(responseCode = "400", description = "Аутентификация не удалась",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                    }
            )
    })
    @PostMapping("login")
    public Mono<ResponseEntity<JwtTokenDto>> signIn(@Valid @RequestBody UserAuthDto userAuthDto) {
        return authService.signIn(userAuthDto)
                .flatMap(
                        dto -> Mono.just(ResponseEntity.status(HttpStatus.OK).body(dto))
                );
    }

    @Operation(summary = "Создать пользователя", description = "Создается новый пользователь с ролью USER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Создание прошло успешно",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = JwtTokenDto.class))
                    }
            ),
            @ApiResponse(responseCode = "400", description = "Пользователь с таким же именем уже есть базе",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                    }
            )
    })
    @PostMapping("register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserDto> signUp(@Valid @RequestBody UserAuthDto userAuthDto) {
        return authService.signUp(userAuthDto)
                .map(user -> conversionService.convert(user, UserDto.class));
    }
}