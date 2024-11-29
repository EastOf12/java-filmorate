package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.requests.NewFilmRequest;
import ru.yandex.practicum.filmorate.dto.requests.UpdateFilmRequest;
import ru.yandex.practicum.filmorate.mappers.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;


@Service
@Slf4j
public class FilmService {

    private final FilmDbStorage filmDbStorage;


    public FilmService(FilmDbStorage filmDbStorage) {
        this.filmDbStorage = filmDbStorage;
    }

    public FilmDto create(NewFilmRequest newFilm) {
        return FilmMapper.mapToFilmDto(filmDbStorage.createFilm(FilmMapper.mapToFilm(newFilm)));
    }

    public FilmDto update(UpdateFilmRequest updateFilm) {
        return FilmMapper.mapToFilmDto(filmDbStorage.updateFilm(FilmMapper.mapToFilmUpdate(updateFilm)));
    }

    public FilmDto getFilm(Long id) {
        return FilmMapper.mapToFilmDto(filmDbStorage.findById(id));
    }

    public Collection<FilmDto> getAll() {
        Collection<FilmDto> filmsDto = new ArrayList<>();
        Collection<Film> films = filmDbStorage.findAll();

        for (Film film : films) {
            filmsDto.add(FilmMapper.mapToFilmDto(film));
        }

        return filmsDto;
    }


    public void addLike(Long filmId, Long userId) {
        filmDbStorage.addLike(filmId, userId);
    } //Добавляет лайк

    public void deleteLike(Long filmId, Long userId) {
        filmDbStorage.removeLike(filmId, userId);
    } //Удаляет лайк

    public Collection<FilmDto> getPopularFilms(int limit) {
        // Получаем все доступные фильмы с фильтрацией по популярности.
        Collection<Film> films = filmDbStorage.findAllPopular(limit);

        if (films == null || films.isEmpty()) {
            log.info("Фильмы не найдены");
            return new ArrayList<>();
        }

        log.info("Вернули топ популярных фильмов в количестве {}", films.size());
        return films.stream()
                .map(FilmMapper::mapToFilmDto)
                .collect(Collectors.toList());
    }
}
