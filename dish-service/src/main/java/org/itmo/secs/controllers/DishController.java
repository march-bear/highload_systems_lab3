package org.itmo.secs.controllers;

import lombok.AllArgsConstructor;

import org.itmo.secs.model.dto.*;
import org.itmo.secs.model.entities.Dish;
import org.itmo.secs.services.*;
import org.itmo.secs.utils.conf.PagingConf;
import org.itmo.secs.utils.converters.CCPF;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Objects;

@AllArgsConstructor
@RestController
@RequestMapping(value = "dish")
@Tag(name = "Блюда (Dishes API)")
public class DishController {
    private final DishService dishService;
    private final ConversionService conversionService;
    private final JsonConvService jsonConvService;
    private final PagingConf pagingConf;

    @Operation(summary = "Создать новое блюдо", description = "Создается новое блюдо по отправленному DTO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Блюдо было успешно создано",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = DishDto.class))
                }
            ), 
            @ApiResponse(responseCode = "400", description = "Блюдо с таким же именем уже есть базе",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<DishDto> create(@RequestBody DishCreateDto dishCreateDto) {
        return dishService.save(Objects.requireNonNull(conversionService.convert(dishCreateDto, Dish.class)))
            .flatMap(this::reactiveConvertDishToDishDto);
    }

    @Operation(summary = "Изменить блюдо", description = "Изменяет блюдо из БД по отправленному DTO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Успешно изменено"), 
            @ApiResponse(responseCode = "400", description = "Блюдо с именем из DTO уже существует",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            ),
            @ApiResponse(responseCode = "404", description = "Блюдо с id из DTO не было найдено",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @PutMapping
    public Mono<Void> updateName(@RequestBody DishUpdateNameDto dishUpdateNameDto) {
        return dishService.updateName(Objects.requireNonNull(conversionService.convert(dishUpdateNameDto, Dish.class)));
    }

    @Operation(summary = "Найти блюда", description = "При указании id ищет блюдо по id, при неуказании id и указании имени ищет блюда по имени, иначе возвращает список блюд по указанной странице")
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "Если были указаны id или имя, тело содержит соответствующее блюдо, иначе список блюд по указанной странице",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = DishDto.class)),
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DishDto.class)))
                }
            ),
            @ApiResponse(responseCode = "404", description = "Блюдо с указанным именем или ID не было найдено",
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

    public Mono<ResponseEntity<String>> findById(Long id) {
        return dishService.findById(id)
                .flatMap(this::reactiveConvertDishToDishDto)
                .map(dto -> ResponseEntity.ok()
                        .header("Content-Type", "application/json")
                        .body(jsonConvService.conv(dto))
                )
                .switchIfEmpty(Mono.just(new ResponseEntity<>((String) null, HttpStatus.NOT_FOUND)));
    }

    public Mono<ResponseEntity<String>> findByName(String name) {
        return dishService.findByName(name)
                .flatMap(this::reactiveConvertDishToDishDto)
                .map(dto -> ResponseEntity.ok()
                        .header("Content-Type", "application/json")
                        .body(jsonConvService.conv(dto))
                )
                .switchIfEmpty(Mono.just(new ResponseEntity<>((String) null, HttpStatus.NOT_FOUND)));
    }

    public Mono<ResponseEntity<String>> findAll(Integer pageNumber, Integer pageSize) {
        return dishService.findAll(pageNumber, pageSize)
                .map(this::reactiveConvertDishToDishDto)
                .collectList()
                .map(
                        (dishesDto) -> ResponseEntity.ok().header("Content-Type", "application/json").body(jsonConvService.conv(dishesDto))
                );
    }

    @Operation(summary = "Добавляет продукт в блюдо, если оно еще не было в нем", description = "При наличии блюда с указанным ip в базе, добавляет в него продукт")
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "204", 
                description = "Продукт успешно добавлен в меню"
            ),
            @ApiResponse(responseCode = "404", description = "Продукт или блюдо с указанным ID не было найдено",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @PutMapping("/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> addItem(@RequestBody DishAddItemDto dishAddItemDto) {
        return dishService.addItem(dishAddItemDto.itemId(), dishAddItemDto.dishId(), dishAddItemDto.count());
    }

    @Operation(summary = "Удалить блюдо", description = "Удалить блюдо по id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Успшено удалено"),
            @ApiResponse(responseCode = "404", description = "Блюдо с отправленным id не было найдено",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
        @Parameter(description = "ID удаляемого продукта", example = "1", required = true)
        @RequestParam(name="id") Long dishId
    ) {
        return dishService.delete(dishId);
    }

    @Operation(summary = "Удалить продукт из блюда", description = "При наличии блюда с указанным ip удаляет из него продукт с указанным id")
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "204", 
                description = "Продукт успешно удален из блюда"
            ),
            @ApiResponse(responseCode = "404", description = "Продукт или блюдо с указанным ID не было найдено",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @DeleteMapping("/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteItem(
        @Parameter(description = "ID удаляемого продукта", example = "1", required = true)
        @RequestParam(name="item-id") Long itemId,
        @Parameter(description = "ID блюда", example = "2", required = true)
        @RequestParam(name="dish-id") Long dishId
    ) {
        return dishService.deleteItem(itemId, dishId);
    }

    @Operation(summary = "Получить список продуктов в составе блюда", description = "При наличии блюда с указанным ip в базе возвращает список продуктов с граммовками")
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "Тело содержит список продуктов с граммовками в составе блюда с указанным id",
                content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ItemCountDto.class)))
                }
            ),
            @ApiResponse(responseCode = "404", description = "Блюдо с указанным ID не было найдено",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @GetMapping("/items")
    public Flux<ItemCountDto> getItems(
        @Parameter(description = "ID блюда", example = "1", required = true)
        @RequestParam() long id
    ) {
        return dishService.makeListOfItems(id)
                .map(it ->
                        new ItemCountDto(conversionService.convert(it.getFirst(), ItemDto.class), it.getSecond())
                );
    }

    public Mono<DishDto> reactiveConvertDishToDishDto(Dish dish) {
        CCPF ccpf = new CCPF(0, 0, 0, 0);

        return dishService.makeListOfItems(dish.getId())
                .reduce(ccpf, (acc, curr) -> {
                            acc.setCalories(
                                    (int) (acc.getCalories() + (double) curr.getFirst().getCalories() / 100.0 * curr.getSecond())
                            );
                            acc.setCarbs(
                                    (int) (acc.getCarbs() + (double) curr.getFirst().getCarbs() / 100.0 * curr.getSecond())
                            );
                            acc.setProtein(
                                    (int) (acc.getProtein() + (double) curr.getFirst().getProtein() / 100.0 * curr.getSecond())
                            );
                            acc.setFats(
                                    (int) (acc.getFats() + (double) curr.getFirst().getFats() / 100.0 * curr.getSecond())
                            );

                            return acc;
                        }
                )
                .map(ccpf_ -> new DishDto(
                        dish.getId(),
                        dish.getName(),
                        ccpf_.getCalories(),
                        ccpf_.getCarbs(),
                        ccpf_.getProtein(),
                        ccpf_.getFats()
                ));
    }
}
