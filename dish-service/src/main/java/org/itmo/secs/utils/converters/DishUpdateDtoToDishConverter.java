package org.itmo.secs.utils.converters;

import lombok.AllArgsConstructor;
import org.itmo.secs.model.dto.DishUpdateNameDto;
import org.itmo.secs.model.entities.Dish;
import org.itmo.secs.services.DishService;
import org.itmo.secs.utils.exceptions.ItemNotFoundException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DishUpdateDtoToDishConverter implements Converter<DishUpdateNameDto, Dish> {
    private final DishService dishService;
    @Override
    public Dish convert(DishUpdateNameDto dishUpdateNameDto) {
        Dish ret = dishService.findById(dishUpdateNameDto.id())
                .blockOptional()
                .orElseThrow(() ->
                        new ItemNotFoundException("Dish with id " + dishUpdateNameDto.id() + " was not found"));

        ret.setName(dishUpdateNameDto.name());
        return ret;
    }
}
