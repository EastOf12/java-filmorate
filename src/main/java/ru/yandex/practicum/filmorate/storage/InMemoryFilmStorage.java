package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.LikesDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@Component
@RequiredArgsConstructor

public class InMemoryFilmStorage implements FilmStorage {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final int MAX_GENRE = 6;
    private static final Map<Integer, String> allMpa = Map.of(
            1, "G",
            2, "PG",
            3, "PG-13",
            4, "R",
            5, "NC-17"
    );
    private final FilmDbStorage filmDbStorage;
    private final UserDbStorage userDbStorage;
    private final LikesDbStorage likesDbStorage;

    @Override
    public Film create(Film film) {
        log.trace("Получен запрос на добавление нового фильма");

        //Проходим валидацию полей.
        passValidationCreate(film);
        log.debug("Валидация пройдена.");

        //Сохраняем новый фильм в БД
        filmDbStorage.createFilm(film);

        log.info("Добавлен новый фильм {}", film.getId());
        return film;
    }

    @Override
    public Film update(Film updateFilm) {

        log.trace("Получен запрос на обновление информации по фильму");

        //Проверяем корректность переданного ID
        if (updateFilm.getId() == null) {
            log.warn("Валидация не пройдена. Id должен быть указан");
            throw new ValidationException("Id должен быть указан");
        }

        //Достаем фильм по его id
        Film film = filmDbStorage.findById(updateFilm.getId());

        //Валидируем поля
        if (updateFilm.getName() != null && !updateFilm.getName().isBlank()) {
            film.setName(updateFilm.getName());
        }

        if (updateFilm.getDescription() != null && !updateFilm.getDescription().isBlank() && updateFilm
                .getDescription().length() <= 200) {
            film.setDescription(updateFilm.getDescription());
        }

        if (updateFilm.getReleaseDate() != null && !updateFilm.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            film.setReleaseDate(updateFilm.getReleaseDate());
        }

        if (updateFilm.getDuration() > 0) {
            film.setDuration(updateFilm.getDuration());
        }


        if (Objects.nonNull(updateFilm.getGenres())) {
            film.setGenres(updateFilm.getGenres());
        }

        //Обновляем фильм в БД
        filmDbStorage.updateFilm(film);

        log.info("Обновлена информация по фильму {}", film.getId());

        return film;
    }

    @Override
    public Film getFilm(Long id) {
        return filmDbStorage.findById(id);
    }

    @Override
    public Collection<Film> getAll() {
        log.info("Отправили информацию по все фильмам.");
        return filmDbStorage.findAll();
    }

    @Override
    public void addLike(Long filmId, Long userId) {

        //Проверяем, что пользователь и фильм существуют.
        userDbStorage.findById(userId);
        filmDbStorage.findById(filmId);

        //Добавляем лайк в БД.
        likesDbStorage.addLike(filmId, userId);
        likesDbStorage.addLike(userId, filmId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {

        //Проверяем, что фильм и пользователь существуют
        User user = userDbStorage.findById(userId);
        Film film = filmDbStorage.findById(filmId);

        if (user == null) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        if (film == null) {
            log.warn("Нет фильма с id {}", filmId);
            throw new NotFoundException("Нет фильма с id " + filmId);
        }

        if (checkLike(filmId, userId)) {
            likesDbStorage.removeLike(userId, filmId);
            log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
        } else {
            log.warn("Пользователь {} не ставил лайк фильму {}", userId, filmId);
            throw new NotFoundException("Пользователь " + userId + " не ставил лайк фильму " + filmId);
        }
    }

    @Override
    public Collection<Mpa> getAllRatings() {
        return filmDbStorage.getAllRatings();
    }

    @Override
    public Mpa getRatingBiId(Integer ratingId) {
        return filmDbStorage.getRating(ratingId);
    }

    @Override
    public Collection<Genre> getAllGenres() {
        return filmDbStorage.getAllGenres();
    }

    @Override
    public Genre getGenreBiId(Integer genreId) {

        if (MAX_GENRE < genreId) {
            throw new NotFoundException("Такой жанр не существует");
        }

        return filmDbStorage.getGenre(genreId);
    }

    private boolean checkLike(Long filmId, Long userId) {
        return likesDbStorage.checkLike(userId, filmId) == 1;
    } //Проверяем, ставил ли пользователь лайк.

    private void passValidationCreate(Film film) {
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

