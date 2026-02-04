package org.itmo.secs.utils.converters;

import lombok.AllArgsConstructor;
import org.itmo.secs.model.dto.DishCreateDto;
import org.itmo.secs.model.entities.Dish;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DishCreateDtoToDishConverter implements Converter<DishCreateDto, Dish> {
    @Override
    public Dish convert(DishCreateDto dishCreateDto) {
        Dish dish = new Dish();
        dish.setName(dishCreateDto.name());
        return dish;
    }
}
