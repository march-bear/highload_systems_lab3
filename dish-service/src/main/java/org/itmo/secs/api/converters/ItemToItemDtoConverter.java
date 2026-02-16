package org.itmo.secs.api.converters;
import lombok.AllArgsConstructor;
import org.itmo.secs.domain.model.dto.ItemDto;
import org.itmo.secs.domain.model.entities.Item;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ItemToItemDtoConverter implements Converter<Item, ItemDto> {
    @Override
    public ItemDto convert(Item i) {
        return new ItemDto(
            i.getId(), 
            i.getName(), 
            i.getCarbs(), 
            i.getProtein(), 
            i.getFats(), 
            i.getCalories()
        );
    }
}
