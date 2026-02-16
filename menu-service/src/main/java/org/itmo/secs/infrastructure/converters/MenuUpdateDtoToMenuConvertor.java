package org.itmo.secs.infrastructure.converters;

import lombok.AllArgsConstructor;
import org.itmo.secs.model.dto.MenuUpdateDto;
import org.itmo.secs.model.entities.Menu;
import org.itmo.secs.model.entities.enums.Meal;
import org.itmo.secs.infrastructure.exceptions.DataIntegrityViolationException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MenuUpdateDtoToMenuConvertor implements Converter<MenuUpdateDto, Menu> {
    @Override
    public Menu convert(MenuUpdateDto menuDto) {
        Menu menu = new Menu();

        menu.setId(menuDto.id());
        menu.setDate(menuDto.date());
        try {
            menu.setMeal(Meal.valueOf(menuDto.meal().trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new DataIntegrityViolationException("Meal value should be BREAKFAST, LUNCH or DINNER");
        }
        return menu;
    }
}
