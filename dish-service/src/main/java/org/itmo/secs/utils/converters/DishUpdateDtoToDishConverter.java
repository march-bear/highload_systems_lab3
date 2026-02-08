package org.itmo.secs.utils.converters;

import lombok.AllArgsConstructor;
import org.itmo.secs.model.dto.DishUpdateNameDto;
import org.itmo.secs.model.entities.Dish;
import org.itmo.secs.services.DishService;
import org.itmo.secs.utils.exceptions.ItemNotFoundException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class DishUpdateDtoToDishConverter implements Converter<DishUpdateNameDto, Dish> {
    private final DishService dishService;
    @Override
    public Dish convert(DishUpdateNameDto dishUpdateNameDto) {
        Dish dish = new Dish();
        dishService.findById(dishUpdateNameDto.id())
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Dish with id " + dishUpdateNameDto.id() + " was not found")))
                .subscribe(it -> {
                    dish.setId(it.getId());
                    dish.setName(dishUpdateNameDto.name());
                });

        return dish;
    }
}
