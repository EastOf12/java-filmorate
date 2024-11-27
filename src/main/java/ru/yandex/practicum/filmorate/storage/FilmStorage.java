package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;


public interface FilmStorage {
    Film create(Film film);

    Film update(Film updateFilm);

    Film getFilm(Long id);

    Collection<Film> getAll();

    Collection<Film> findAllPopular();

    void addLike(Long filmId, Long userId);

    void removeLike(Long userId, Long friendId);
}
