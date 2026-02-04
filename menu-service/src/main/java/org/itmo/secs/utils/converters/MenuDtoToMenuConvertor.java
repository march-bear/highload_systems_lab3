package org.itmo.secs.utils.converters;

import lombok.AllArgsConstructor;
import org.itmo.secs.model.dto.MenuDto;
import org.itmo.secs.model.entities.Menu;
import org.itmo.secs.model.entities.enums.Meal;
import org.itmo.secs.utils.exceptions.DataIntegrityViolationException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class MenuDtoToMenuConvertor implements Converter<MenuDto, Menu> {
    @Override
    public Menu convert(MenuDto menuDto) {
        Menu menu = new Menu();

        menu.setId(menuDto.id());
        menu.setUserId(menuDto.id());
        menu.setDate(menuDto.date());
        try {
            menu.setMeal(Meal.valueOf(menuDto.meal().trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new DataIntegrityViolationException("Meal value should be BREAKFAST, LUNCH or DINNER");
        }
        return menu;
    }
}
