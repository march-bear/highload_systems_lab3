package org.itmo.secs.api.converters;

import lombok.AllArgsConstructor;
import org.itmo.secs.domain.model.dto.ItemUpdateDto;
import org.itmo.secs.domain.model.entities.Item;
import org.itmo.secs.application.services.ItemService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ItemUpdateDtoToItemConverter implements Converter<ItemUpdateDto, Item> {
    private final ItemService itemService;

    @Override
    public Item convert(ItemUpdateDto itemUpdateDto) {
        Item ret = new Item();

        ret.setId(itemUpdateDto.id());
        ret.setName(itemUpdateDto.name());
        ret.setCalories(itemUpdateDto.calories());
        ret.setCarbs(itemUpdateDto.carbs());
        ret.setProtein(itemUpdateDto.protein());
        ret.setFats(itemUpdateDto.fats());

        return ret;
    }
}
