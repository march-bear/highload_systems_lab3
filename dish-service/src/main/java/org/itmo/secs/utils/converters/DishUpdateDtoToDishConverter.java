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
    @Override
    public Dish convert(DishUpdateNameDto dishUpdateNameDto) {
        Dish dish = new Dish();
        dish.setId(dishUpdateNameDto.id());
        dish.setName(dishUpdateNameDto.name());

        return dish;
    }
}
