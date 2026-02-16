package org.itmo.secs.api.converters;

import lombok.AllArgsConstructor;
import org.itmo.secs.domain.model.dto.DishUpdateNameDto;
import org.itmo.secs.domain.model.entities.Dish;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DishUpdateDtoToDishConverter implements Converter<DishUpdateNameDto, Dish> {
    @Override
    public Dish convert(DishUpdateNameDto dishUpdateNameDto) {
        Dish dish = new Dish();
        dish.setId(dishUpdateNameDto.id());
        dish.setName(dishUpdateNameDto.name());

        return dish;
    }
}
