package ru.yandex.practicum.filmorate.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.dto.requests.NewUserRequest;
import ru.yandex.practicum.filmorate.dto.requests.UpdateUserRequest;
import ru.yandex.practicum.filmorate.model.User;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserMapper {

    public static User mapToUser(NewUserRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setLogin(request.getLogin());
        user.setBirthday(request.getBirthday());
        return user;
    }

    public static UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setLogin(user.getLogin());
        dto.setBirthday(user.getBirthday());


        if (Objects.nonNull(user.getFriends())) {
            Set<Long> friendsIds = new HashSet<>(user.getFriends());
            dto.setFriends(friendsIds);
        }

        return dto;
    }

    public static User mapToUserUpdate(UpdateUserRequest request) {
        User user = new User();

        if (request.getId() != null) {
            user.setId(request.getId());
        }
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getLogin() != null) {
            user.setLogin(request.getLogin());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
        }
        return user;
    }
}
