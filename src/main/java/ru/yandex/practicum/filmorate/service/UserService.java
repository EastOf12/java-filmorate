package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.dto.requests.NewUserRequest;
import ru.yandex.practicum.filmorate.dto.requests.UpdateUserRequest;
import ru.yandex.practicum.filmorate.mappers.UserMapper;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.Collection;

@Service
@Slf4j
public class UserService {

    private final UserDbStorage userDbStorage;


    public UserService(UserDbStorage userDbStorage) {
        this.userDbStorage = userDbStorage;
    }

    public UserDto create(NewUserRequest userRequest) {
        return UserMapper.mapToUserDto(userDbStorage.createUser(UserMapper.mapToUser(userRequest)));
    }

    public UserDto update(UpdateUserRequest updateUserRequest) {
        return UserMapper.mapToUserDto(userDbStorage.updateUser(UserMapper.mapToUserUpdate(updateUserRequest)));
    }

    public Collection<UserDto> getAll() {
        Collection<User> allUser = userDbStorage.findAll();
        Collection<UserDto> allUserDto = new ArrayList<>();

        for (User user : allUser) {
            allUserDto.add(UserMapper.mapToUserDto(user));
        }

        return allUserDto;
    }

    public void addFriend(Long userId, Long friendId) {
        userDbStorage.addFriend(userId, friendId);
    } //Добавить пользователя в друзья

    public void removeFriend(Long userId, Long friendId) {
        userDbStorage.removeFriend(userId, friendId);
    } //Удалить пользователя из друзей

    public Collection<UserDto> getAllUserFriends(Long userId) {

        //Проверяем, что такой пользователь существует
        userDbStorage.findById(userId);

        //Получаем друзей этого пользователя
        Collection<User> allUserFriends = userDbStorage.getAllUserFriends(userId);
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
        User user = userDbStorage.findById(userId);
        User otherUser = userDbStorage.findById(otherUserId);

        Collection<UserDto> allUserDto = new ArrayList<>();
        Collection<User> allUser = userDbStorage.getFriendsCommon(user.getId(), otherUser.getId());

        for (User us : allUser) {
            allUserDto.add(UserMapper.mapToUserDto(us));
        }

        return allUserDto;
    } //Возвращает общих друзей пользователей.
}
