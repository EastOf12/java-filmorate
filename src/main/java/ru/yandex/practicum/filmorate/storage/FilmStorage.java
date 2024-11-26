package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.Collection;


public interface FilmStorage {
    Film create(Film film);

    Film update(Film updateFilm);

    Film getFilm(Long id);

    Collection<Film> getAll();

    void addLike(Long filmId, Long userId);

    void removeLike(Long userId, Long friendId);

    Collection<Mpa> getAllRatings();

    Mpa getRatingBiId(Integer ratingId);

    Collection<Genre> getAllGenres();

    Genre getGenreBiId(Integer ratingId);
}
