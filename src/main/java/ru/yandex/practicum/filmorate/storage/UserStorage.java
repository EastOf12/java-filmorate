package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;

public interface UserStorage {
    User create(User user);

    User update(User updateUser);

    Collection<User> getAll();

    boolean checkUserAvailability(Long userId);

    User getUser(Long userId);
}
