package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.requests.NewFilmRequest;
import ru.yandex.practicum.filmorate.dto.requests.UpdateFilmRequest;
import ru.yandex.practicum.filmorate.mappers.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;


@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;


    public FilmService(FilmStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    public FilmDto create(NewFilmRequest newFilm) {
        return FilmMapper.mapToFilmDto(filmStorage.create(FilmMapper.mapToFilm(newFilm)));
    }

    public FilmDto update(UpdateFilmRequest updateFilm) {
        return FilmMapper.mapToFilmDto(filmStorage.update(FilmMapper.mapToFilmUpdate(updateFilm)));
    }

    public FilmDto getFilm(Long id) {
        return FilmMapper.mapToFilmDto(filmStorage.getFilm(id));
    }

    public Collection<FilmDto> getAll() {
        Collection<FilmDto> filmsDto = new ArrayList<>();
        Collection<Film> films = filmStorage.getAll();

        for (Film film : films) {
            filmsDto.add(FilmMapper.mapToFilmDto(film));
        }

        return filmsDto;
    }


    public void addLike(Long filmId, Long userId) {
        filmStorage.addLike(filmId, userId);
    } //Добавляет лайк

    public void deleteLike(Long filmId, Long userId) {
        filmStorage.removeLike(filmId, userId);
    } //Удаляет лайк

    public Collection<FilmDto> getPopularFilms(int count) {
        // Получаем все доступные фильмы с фильтрацией по популярности.
        Collection<Film> films = filmStorage.findAllPopular();

        if (films == null || films.isEmpty()) {
            log.info("Фильмы не найдены");
            return new ArrayList<>();
        }

        // Оставляем нужное количество фильмов
        log.info("Вернули топ популярных фильмов в количестве {}", count);

        // Создаем новую коллекцию только с нужным количеством популярных фильмов
        return films.stream()
                .limit(count)
                .map(FilmMapper::mapToFilmDto)
                .collect(Collectors.toList());
    }
}
