package org.itmo.secs.api.converters;

import lombok.AllArgsConstructor;
import org.itmo.secs.domain.model.dto.MenuCreateDto;
import org.itmo.secs.domain.model.entities.Menu;
import org.itmo.secs.domain.model.entities.enums.Meal;
import org.itmo.secs.exception.DataIntegrityViolationException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MenuCreateDtoToMenuConvertor implements Converter<MenuCreateDto, Menu> {
    @Override
    public Menu convert(MenuCreateDto menuDto) {
        Menu menu = new Menu();

        menu.setDate(menuDto.date());
        try {
            menu.setMeal(Meal.valueOf(menuDto.meal().trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new DataIntegrityViolationException("Meal value should be BREAKFAST, LUNCH or DINNER");
        }
        return menu;
    }
}
