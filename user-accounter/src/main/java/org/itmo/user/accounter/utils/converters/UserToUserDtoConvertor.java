package org.itmo.user.accounter.utils.converters;

import lombok.AllArgsConstructor;
import org.itmo.user.accounter.model.dto.UserDto;
import org.itmo.user.accounter.model.entities.User;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserToUserDtoConvertor implements Converter<User, UserDto> {
    @Override
    public UserDto convert(User user) {
        return new UserDto(user.getId(), user.getName());
    }
}
