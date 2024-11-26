package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.dto.requests.NewUserRequest;
import ru.yandex.practicum.filmorate.dto.requests.UpdateUserRequest;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.Collection;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody NewUserRequest user) {
        return userService.create(user);
    } //Создать пользователя

    @PutMapping
    public UserDto update(@RequestBody UpdateUserRequest updateUserRequest) {
        return userService.update(updateUserRequest);
    } //Обновить пользователя

    @GetMapping
    public Collection<UserDto> getAll() {
        return userService.getAll();
    } //Получить всех пользователей


    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        userService.addFriend(id, friendId);
    } //Добавить пользователя в друзья

    @DeleteMapping("/{id}/friends/{friendId}")
    public void removeFriend(@PathVariable Long id, @PathVariable Long friendId) {
        userService.removeFriend(id, friendId);
    } //Удалить пользователя из друзей

    @GetMapping("/{id}/friends")
    public Collection<UserDto> getUserFriends(@PathVariable Long id) {
        return userService.getAllUserFriends(id);
    } //Получить всех друзей пользователя

    @GetMapping("/{id}/friends/common/{otherId}")
    public Collection<UserDto> getFriendsCommon(@PathVariable Long id, @PathVariable Long otherId) {
        return userService.getFriendsCommon(id, otherId);
    } //Возвращает список друзей общий с другим пользователем.
}
