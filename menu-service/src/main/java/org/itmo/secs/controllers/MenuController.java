package org.itmo.secs.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.itmo.secs.model.dto.*;
import org.itmo.secs.model.entities.Menu;
import org.itmo.secs.services.JsonConvService;
import org.itmo.secs.services.MenuService;
import org.itmo.secs.utils.conf.PagingConf;
import org.itmo.secs.utils.converters.CCPF;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@AllArgsConstructor
@RestController
@RequestMapping(value = "menu")
@Tag(name = "Меню (Menus API)")
public class MenuController {
    private MenuService menuService;
    private ConversionService conversionService;
    private final JsonConvService jsonConvService;
    private final PagingConf pagingConf;

    @Operation(summary = "Создать новое меню", description = "Создается новое меню по отправленному DTO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Меню было успешно создано",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MenuDto.class))
                }
            ), 
            @ApiResponse(responseCode = "400", description = "Меню с такой же комбинацией ДАТА-ПОЛЬЗОВАТЕЛЬ-ПРИЕМ ПИЩИ уже есть базе",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @Parameter(name = "userId", hidden = true)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MenuDto> create(@RequestHeader("X-User-Id") Long userId, @RequestBody MenuCreateDto menuDto) {
        return menuService.save(Objects.requireNonNull(conversionService.convert(menuDto, Menu.class)))
                .flatMap(this::reactiveConvertMenuToMenuDto);
    }

    @Operation(summary = "Изменить меню", description = "Изменяет меню из БД по отправленному DTO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Успешно изменено"), 
            @ApiResponse(responseCode = "400", description = "Меню с такой же комбинацией ДАТА-ПОЛЬЗОВАТЕЛЬ-ПРИЕМ ПИЩИ уже есть базе",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            ),
            @ApiResponse(responseCode = "404", description = "Меню с id из DTO не было найдено",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @Parameter(name = "userId", hidden = true)
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> update(@RequestHeader("X-User-Id") Long userId, @RequestBody MenuDto menuDto) {
        menuService.update(Objects.requireNonNull(conversionService.convert(menuDto, Menu.class)));
        return Mono.empty();
    }

    @Operation(summary = "Удалить меню", description = "Удалить меню по id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Успшено удален"),
            @ApiResponse(responseCode = "404", description = "Меню с отправленным id не было найдено",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @Parameter(name = "userId", hidden = true)
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "ID удаляемого меню", example = "1", required = true)
            @RequestParam(name="id") Long menuId
    ) {
        menuService.delete(menuId);
        return Mono.empty();
    }

    @Operation(summary = "Найти меню", description = "При указании id ищет продукт по id, иначе возвращает список продуктов по указанной странице")
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "Если было указано id, тело содержит соответствующее меню, иначе список из меню по указанной странице или юзернейму",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MenuDto.class)),
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MenuDto.class)))
                }
            ),
            @ApiResponse(responseCode = "404", description = "Меню с указанным ID не было найдено",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @GetMapping
    @Parameter(name = "userId", hidden = true)
    @Parameter(name = "userRole", hidden = true)
    public Mono<ResponseEntity<String>> find(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @Parameter(description = "ID продукта", example = "1")
            @RequestParam(required=false) Long id,
            @Parameter(description = "Номер страницы (нумерация с 0)", example = "0")
            @RequestParam(name="pnumber", required=false) Integer _pageNumber,
            @Parameter(description = "Размер страницы (по умолчанию 50)", example = "10")
            @RequestParam(name="psize", required=false) Integer _pageSize
    ) {
        if (id != null) {
            return findById(id);
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

    public Mono<ResponseEntity<String>> findAllByUsername(String username) {
        return menuService.findAllByUsername(username)
                .flatMap(this::reactiveConvertMenuToMenuDto)
                .collectList()
                .map(menusDto -> ResponseEntity.ok(jsonConvService.conv(menusDto)));
    }

    public Mono<ResponseEntity<String>> findAll(Integer pageNumber, Integer pageSize) {
        return menuService.findAll(pageNumber, pageSize)
        .flatMap(this::reactiveConvertMenuToMenuDto)
        .collectList()
        .map(menusDto -> ResponseEntity.ok(jsonConvService.conv(menusDto)));
    }

    public Mono<ResponseEntity<String>> findById(Long id) {
        return menuService.findById(id)
                .flatMap(this::reactiveConvertMenuToMenuDto)
                .map(dto -> ResponseEntity.ok(jsonConvService.conv(dto)))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Добавляет блюдо в меню, если оно еще не было в нем", description = "При наличии меню с указанным ip в базе, добавляет в него блюдо")
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "204", 
                description = "Блюдо успешно добавлено в меню"
            ),
            @ApiResponse(responseCode = "404", description = "Меню или блюдо с указанным ID не было найдено",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @Parameter(name = "userId", hidden = true)
    @PutMapping("/dishes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> addDish(@RequestHeader("X-User-Id") Long userId, @RequestBody MenuDishDto dto) {
        return menuService.includeDishToMenu(dto.dishId(), dto.menuId());
    }

    @Operation(summary = "Получить список блюд в составе меню", description = "При наличии меню с указанным ip в базе возвращает список блюд в нем")
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "Тело содержит список блюд в меню с указанным id",
                content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DishDto.class)))
                }
            ),
            @ApiResponse(responseCode = "404", description = "Меню с указанным ID не было найдено",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @Parameter(name = "userId", hidden = true)
    @GetMapping("/dishes")
    public Flux<DishDto> getDishes(
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "ID меню", example = "1", required = true)
            @RequestParam() Long id
    ) {
        return menuService.makeListOfDishes(id)
                .map((it) -> Objects.requireNonNull(conversionService.convert(it, DishDto.class)));
    }

    @Operation(summary = "Удалить блюдо из меню", description = "При наличии меню с указанным ip удаляет из него блюдо с указанным id")
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "204", 
                description = "Блюдо успешно удалено из меню"
            ),
            @ApiResponse(responseCode = "404", description = "Меню или блюдо с указанным ID не было найдено",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                }
            )
        })
    @Parameter(name = "userId", hidden = true)
    @DeleteMapping("/dishes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteDish(@RequestHeader("X-User-Id") Long userId, @RequestBody MenuDishDto dto) {
        menuService.deleteDishFromMenu(dto.dishId(), dto.menuId());
        return Mono.empty();
    }

    public Mono<MenuDto> reactiveConvertMenuToMenuDto(Menu menu) {
        CCPF ccpf = new CCPF(0, 0, 0, 0);
        return menuService.makeListOfDishes(menu.getId())
                .reduce(ccpf, (acc, curr) -> {
                            acc.setCalories(
                                    acc.getCalories() + curr.calories()
                            );
                            acc.setCarbs(
                                    acc.getCarbs() + curr.carbs()
                            );
                            acc.setProtein(
                                    acc.getProtein() + curr.protein()
                            );
                            acc.setFats(
                                    acc.getFats() + curr.fats()
                            );

                            return acc;
                        }
                ).map(ccpf_ -> new MenuDto(
                            menu.getId(),
                            menu.getDate(),
                            menu.getMeal().toString(),
                            ccpf_.getCalories(),
                            ccpf_.getCarbs(),
                            ccpf_.getProtein(),
                            ccpf_.getFats()
                    )
                );
    }
}
