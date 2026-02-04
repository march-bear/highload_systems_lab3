package org.itmo.secs.controllers;

import lombok.AllArgsConstructor;

import org.itmo.secs.model.dto.*;
import org.itmo.secs.model.entities.Item;
import org.itmo.secs.services.ItemService;
import org.itmo.secs.services.JsonConvService;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.*;

import org.itmo.secs.utils.conf.PagingConf;

import reactor.core.publisher.Mono;

import java.util.Objects;

@AllArgsConstructor
@RestController
@RequestMapping(value = "item")
@Tag(name = "Продукты (Items API)")
public class ItemController {
    private final ConversionService conversionService;
    private final ItemService itemService;
    private final JsonConvService jsonConvService;
    private final PagingConf pagingConf;

    @Operation(summary = "Создать новый продукт", description = "Создается новый пользователь по отправленному ItemCreateDTO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Продукт был успешно создан",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ItemDto.class))
                }
            ), 
            @ApiResponse(responseCode = "400", description = "Продукт с таким же именем уже есть базе",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ItemDto> create(@RequestBody ItemCreateDto itemCreateDto) {
        return itemService.save(Objects.requireNonNull(conversionService.convert(itemCreateDto, Item.class)))
                .map(item ->
                        Objects.requireNonNull(conversionService.convert(item, ItemDto.class))
                );
    }

    @Operation(summary = "Изменить продукт", description = "Изменяет продукт из БД по отправленному DTO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Успешно изменен"), 
            @ApiResponse(responseCode = "400", description = "Продукт с именем из DTO уже существует",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            ),
            @ApiResponse(responseCode = "404", description = "Продукт с id из DTO не был найден",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> update(@RequestBody ItemUpdateDto itemUpdateDto) {
        return itemService.update(Objects.requireNonNull(conversionService.convert(itemUpdateDto, Item.class)));
    }

    @Operation(summary = "Удалить продукт", description = "Удалить продукт по id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Успшено удален"),
            @ApiResponse(responseCode = "404", description = "Продукт с отправленным id не был найден",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
        @Parameter(description = "ID удаляемого продукта", example = "1", required = true)
        @RequestParam(name="id") Long itemId
    ) {
        return itemService.delete(itemId);
    }

    @Operation(summary = "Найти продукты", description = "При указании id ищет продукт по id, при неуказании id и указании имени ищет продукт по имени, иначе возвращает список продуктов по указанной странице")
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "Если были указаны id или имя, тело содержит соответствующий продукт, иначе список продуктов по указанной странице",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ItemDto.class)),
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ItemDto.class)))
                }
            ),
            @ApiResponse(responseCode = "404", description = "Продукт с указанным именем или ID не был найден",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @GetMapping
    public Mono<ResponseEntity<String>> find(
        @Parameter(description = "ID продукта", example = "1")
        @RequestParam(required=false) Long id,
        @Parameter(description = "Номер страницы (нумерация с 0)", example = "0")
        @RequestParam(name="pnumber", required=false) Integer _pageNumber,
        @Parameter(description = "Размер страницы (по умолчанию 50)", example = "10")
        @RequestParam(name="psize", required=false) Integer _pageSize,
        @Parameter(description = "Имя продукта", example = "Творог")
        @RequestParam(required=false) String name
    ) {
        if (id != null) {
            return findById(id);
        } else if (name != null) {
            return findByName(name);
        } else {
            Integer pageNumber = (_pageNumber == null) ? 0 : _pageNumber;
            Integer pageSize = (_pageSize == null) 
                ? pagingConf.getDefaultPageSize()
                : (_pageSize > pagingConf.getMaxPageSize())
                    ? pagingConf.getMaxPageSize()
                    : _pageSize;

            return findAll(pageNumber, pageSize);
        }
    }

    public Mono<ResponseEntity<String>> findAll(Integer pageNumber, Integer pageSize) {
        return Mono.zip(
            itemService.findAll(pageNumber, pageSize)
            .map((it) -> Objects.requireNonNull(conversionService.convert(it, ItemDto.class)))
            .collectList(),
            itemService.count(), (itemsDto, count) ->
                ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(count))
                .body(jsonConvService.conv(itemsDto))
            );
    }

    public Mono<ResponseEntity<String>> findById(Long id) {
        return itemService.findById(id)
                .map((item) -> ResponseEntity.ok(jsonConvService.conv(
                        conversionService.convert(item, ItemDto.class)
                    )))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    public Mono<ResponseEntity<String>> findByName(String name) {
        return itemService.findByName(name)
                .map((item) -> ResponseEntity.ok(jsonConvService.conv(
                        conversionService.convert(item, ItemDto.class)
                    )))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}