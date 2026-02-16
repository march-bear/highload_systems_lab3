package org.itmo.secs.api.converters;

import lombok.AllArgsConstructor;
import org.itmo.secs.domain.model.dto.ItemCreateDto;
import org.itmo.secs.domain.model.entities.Item;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ItemCreateDtoToItemConverter implements Converter<ItemCreateDto, Item> {
    @Override
    public Item convert(ItemCreateDto itemCreateDto) {
        Item ret = new Item();
        ret.setName(itemCreateDto.name());
        ret.setCalories(itemCreateDto.calories());
        ret.setCarbs(itemCreateDto.carbs());
        ret.setProtein(itemCreateDto.protein());
        ret.setFats(itemCreateDto.fats());
        return ret;
    }
}
