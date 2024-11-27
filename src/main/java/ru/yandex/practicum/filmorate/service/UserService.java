package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.dto.requests.NewUserRequest;
import ru.yandex.practicum.filmorate.dto.requests.UpdateUserRequest;
import ru.yandex.practicum.filmorate.mappers.UserMapper;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.ArrayList;
import java.util.Collection;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;


    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public UserDto create(NewUserRequest userRequest) {
        User user = UserMapper.mapToUser(userRequest);
        return UserMapper.mapToUserDto(userStorage.create(user));
    }

    public UserDto update(UpdateUserRequest updateUserRequest) {
        User user = UserMapper.mapToUserUpdate(updateUserRequest);
        return UserMapper.mapToUserDto(userStorage.update(user));
    }

    public Collection<UserDto> getAll() {
        Collection<User> allUser = userStorage.getAll();
        Collection<UserDto> allUserDto = new ArrayList<>();

        for (User user : allUser) {
            allUserDto.add(UserMapper.mapToUserDto(user));
        }

        return allUserDto;
    }

    public void addFriend(Long userId, Long friendId) {
        userStorage.addFriend(userId, friendId);
    } //Добавить пользователя в друзья

    public void removeFriend(Long userId, Long friendId) {
        userStorage.removeFriend(userId, friendId);
    } //Удалить пользователя из друзей

    public Collection<UserDto> getAllUserFriends(Long userId) {

        //Проверяем, что такой пользователь существует
        userStorage.getUser(userId);

        //Получаем друзей этого пользователя
        Collection<User> allUserFriends = userStorage.getAllUserFriends(userId);
        Collection<UserDto> allUserDto = new ArrayList<>();

        if (allUserFriends.isEmpty()) {
            log.info("У пользователя {} нет друзей", userId);
            return new ArrayList<>();
        } else {
            for (User us : allUserFriends) {
                allUserDto.add(UserMapper.mapToUserDto(us));
            }

            return allUserDto;
        }
    } //Получить всех друзей пользователя

    public Collection<UserDto> getFriendsCommon(Long userId, Long otherUserId) {
        //Проверяем, что такие пользователи существуют.
        User user = userStorage.getUser(userId);
        User otherUser = userStorage.getUser(otherUserId);

        Collection<UserDto> allUserDto = new ArrayList<>();
        Collection<User> allUser = userStorage.getFriendsCommon(user.getId(), otherUser.getId());

        for (User us : allUser) {
            allUserDto.add(UserMapper.mapToUserDto(us));
        }

        return allUserDto;
    } //Возвращает общих друзей пользователей.
}
