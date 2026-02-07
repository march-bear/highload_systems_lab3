package org.itmo.secs.utils.converters;

import lombok.AllArgsConstructor;
import org.itmo.secs.model.dto.MenuCreateDto;
import org.itmo.secs.model.entities.Menu;
import org.itmo.secs.model.entities.enums.Meal;
import org.itmo.secs.utils.exceptions.DataIntegrityViolationException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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
