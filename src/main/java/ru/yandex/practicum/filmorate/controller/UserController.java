package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserStorage userStorage;
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@RequestBody User user) {
        return userStorage.create(user);
    } //Создать пользователя

    @PutMapping
    public User update(@RequestBody User newUser) {
        return userStorage.update(newUser);
    } //Обновить пользователя

    @GetMapping
    public Collection<User> getAll() {
        return userStorage.getAll();
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
    public Collection<User> getUserFriends(@PathVariable Long id) {
        return userService.getAllUserFriends(id);
    } //Получить всех друзей пользователя

    @GetMapping("/{id}/friends/common/{otherId}")
    public Collection<User> getFriendsCommon(@PathVariable Long id, @PathVariable Long otherId) {
        return userService.getFriendsCommon(id, otherId);
    } //Возвращает список друзей общий с другим пользователем.
}
