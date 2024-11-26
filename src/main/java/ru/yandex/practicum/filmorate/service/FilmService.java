package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.dto.MpaDto;
import ru.yandex.practicum.filmorate.dto.requests.NewFilmRequest;
import ru.yandex.practicum.filmorate.dto.requests.UpdateFilmRequest;
import ru.yandex.practicum.filmorate.mappers.FilmMapper;
import ru.yandex.practicum.filmorate.mappers.GenreMapper;
import ru.yandex.practicum.filmorate.mappers.MpaMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        // Получаем все доступные фильмы.
        Collection<Film> films = filmStorage.getAll();

        if (films == null || films.isEmpty()) {
            log.info("Фильмы не найдены");
            return new ArrayList<>();
        }

        // Сортируем фильмы по количеству лайков
        List<Film> sortedFilms = new ArrayList<>(films);
        sortedFilms.sort((film1, film2) ->
                Integer.compare(film2.getLikes().size(), film1.getLikes().size())
        );

        // Оставляем нужное количество фильмов
        int size = Math.min(count, sortedFilms.size());
        log.info("Вернули топ популярных фильмов в количестве {}", size);

        // Создаем новую коллекцию только с нужным количеством популярных фильмов
        return sortedFilms.subList(0, size).stream()
                .map(FilmMapper::mapToFilmDto)
                .collect(Collectors.toList());
    } //Возвращает самые популярные фильмы

    public Collection<MpaDto> getAllMpa() {
        List<MpaDto> mpaDtos = new ArrayList<>();
        Collection<Mpa> mpas = filmStorage.getAllRatings();

        for (Mpa mpa : mpas) {
            MpaDto mpaDto = new MpaDto();
            mpaDto.setId(mpa.getId());
            mpaDto.setName(mpa.getName());
            mpaDtos.add(mpaDto);
        }

        mpaDtos.sort((g1, g2) -> Integer.compare(g1.getId(), g2.getId()));

        return mpaDtos;
    }

    public MpaDto getMpa(int id) {
        return MpaMapper.mapToMpaDto(filmStorage.getRatingBiId(id));
    }

    public Collection<GenreDto> getAllGenres() {
        Collection<Genre> genres = filmStorage.getAllGenres();
        List<GenreDto> genreDtos = new ArrayList<>();

        // Заполняем список genreDtos из коллекции жанров
        for (Genre genre : genres) {
            GenreDto genreDto = new GenreDto();
            genreDto.setId(genre.getId());
            genreDto.setName(genre.getName());
            genreDtos.add(genreDto); // Добавляем genreDto в список
        }

        return genreDtos;
    }

    public GenreDto getGenre(int id) {
        return GenreMapper.mapToGenreDto(filmStorage.getGenreBiId(id));
    }
}
