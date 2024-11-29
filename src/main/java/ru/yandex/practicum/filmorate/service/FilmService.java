package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.GenreDbStorage;
import ru.yandex.practicum.filmorate.dal.LikesDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.requests.NewFilmRequest;
import ru.yandex.practicum.filmorate.dto.requests.UpdateFilmRequest;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mappers.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class FilmService {
    private final FilmDbStorage filmDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final LikesDbStorage likesDbStorage;
    private final UserDbStorage userDbStorage;

    public FilmService(FilmDbStorage filmDbStorage, GenreDbStorage genreDbStorage, LikesDbStorage likesDbStorage, UserDbStorage userDbStorage) {
        this.filmDbStorage = filmDbStorage;
        this.genreDbStorage = genreDbStorage;
        this.likesDbStorage = likesDbStorage;
        this.userDbStorage = userDbStorage;
    }

    public FilmDto create(NewFilmRequest newFilmRequest) {
        Film film = FilmMapper.mapToFilm(newFilmRequest);
        passValidationCreate(film);

        Film createdFilm = filmDbStorage.createFilm(film);

        //Добавляем рейтинг к фильму
        filmDbStorage.addRating(film.getId(), film.getMpa().getId());

        //Добавляем жанры к фильму
        Set<Integer> allGenresId = new HashSet<>();
        for (Genre genre : film.getGenres()) {
            allGenresId.add(genre.getId());
        }
        filmDbStorage.addGenres(film.getId(), allGenresId);

        Collection<Genre> genres = genreDbStorage.getAllGenres();
        List<Genre> filmGenres = new ArrayList<>();

        for (Genre genre : genres) {
            for (Integer genreId : allGenresId) {
                if (Objects.equals(genreId, genre.getId())) {
                    filmGenres.add(genre);
                }
            }
        }

        filmGenres.sort((g1, g2) -> Integer.compare(g1.getId(), g2.getId()));
        film.setGenres(filmGenres);

        return FilmMapper.mapToFilmDto(createdFilm);
    }

    public FilmDto update(UpdateFilmRequest updateFilm) {
        return FilmMapper.mapToFilmDto(filmDbStorage.updateFilm(FilmMapper.mapToFilmUpdate(updateFilm)));
    }

    public FilmDto getFilm(Long id) {
        return FilmMapper.mapToFilmDto(filmDbStorage.findById(id));
    }

    public Collection<FilmDto> getAll() {
        Collection<FilmDto> filmsDto = new ArrayList<>();
        List<Film> films = genreDbStorage.addGenres(filmDbStorage.findAll());

        for (Film film : films) {
            filmsDto.add(FilmMapper.mapToFilmDto(film));
        }

        return filmsDto;
    }

    public void addLike(Long filmId, Long userId) {
        userDbStorage.findById(userId); //Проверяем, что пользователь существует
        filmDbStorage.findById(filmId); //Проверяем, что фильм существует
        likesDbStorage.addLike(filmId, userId);
        ;
    } //Добавляет лайк

    public void deleteLike(Long filmId, Long userId) {
        userDbStorage.findById(userId); //Проверяем, что пользователь существует
        filmDbStorage.findById(filmId); //Проверяем, что фильм существует
        likesDbStorage.removeLike(userId, filmId);
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

    private void passValidationCreate(Film film) {
        final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
        final Map<Integer, String> allMpa = Map.of(
                1, "G",
                2, "PG",
                3, "PG-13",
                4, "R",
                5, "NC-17"
        );

        //Проверяем корректность заполнения полей.
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Валидация не пройдена. Не было передано название фильма.");
            throw new ValidationException("Название не может быть пустым.");
        } else if (film.getDescription() == null || film.getDescription().isBlank()) {
            log.warn("Валидация не пройдена. Описание запроса не может быть пустым");
            throw new ValidationException("Описание запроса не может быть пустым");
        } else if (film.getDescription().length() > 200) {
            log.warn("Валидация не пройдена. Количество символов в описании запроса {}",
                    film.getDescription().length());
            throw new ValidationException("Максимальная длина описания — 200 символов.");
        } else if (film.getReleaseDate() == null) {
            log.warn("Валидация не пройдена. Дата в запросе = null");
            throw new ValidationException("Дата в запросе = null");
        } else if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Валидация не пройдена. Дата в запросе {}",
                    film.getReleaseDate());
            throw new ValidationException("Дата релиза — не раньше " + MIN_RELEASE_DATE);
        } else if (film.getDuration() < 1) {
            log.warn("Валидация не пройдена. Продолжительность фильма в запросе {}",
                    film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом.");
        } else if (areGenreMoreRange(film.getGenres())) {
            log.warn("Валидация не пройдена. Передан несуществующий жанр");
            throw new ValidationException("Передан несуществующий жанр");
        } else if (isMpaValidationCorrect(film.getMpa())) {
            film.getMpa().setName(allMpa.get(film.getMpa().getId()));
        }
    }

    private boolean areGenreMoreRange(Collection<Genre> genres) {
        final int MAX_GENRE = 6;

        for (Genre genre : genres) {
            if (genre.getId() <= 0 || genre.getId() > MAX_GENRE) {
                return true;
            }
        }

        return false;
    } //Проверяем, что переданные id жанра существуют.

    private boolean isMpaValidationCorrect(Mpa mpa) {
        final int MAX_MPA_ID = 5;

        if (mpa.getId() > MAX_MPA_ID || mpa.getId() < 1) {
            log.error("Такой рейтинг не существует");
            throw new ValidationException("Передан несуществующий mpa.id");
        }

        return true;
    }
}
