package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private final Map<Long, Film> films = new HashMap<>();

    @Override
    public Film create(Film film) {
        log.trace("Получен запрос на добавление нового фильма");

        //Проходим валидацию полей.
        passValidationCreate(film);
        log.debug("Валидация пройдена.");

        //Сохраняем новый фильм в памяти приложения
        film.setId(getNextId());
        films.put(film.getId(), film);
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
        Film film = films.get(updateFilm.getId());

        //Обновляем информацию на основне новых полей.
        if (film == null) {
            log.warn("Валидация не пройдена. Фильм с id = {} не найден", updateFilm.getId());
            throw new NotFoundException("Фильм с id = " + updateFilm.getId() + " не найден");
        } else {
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

            if (updateFilm.getDuration() < 0) {
                film.setDuration(updateFilm.getDuration());
            }
        }

        log.info("Обновлена информация по фильму {}", updateFilm.getId());
        return updateFilm;
    }

    @Override
    public Collection<Film> getAll() {
        log.info("Отправили информацию по все фильмам.");
        return films.values();
    }

    @Override
    public boolean checkFilmAvailability(Long filmId) {
        return !films.containsKey(filmId);
    }

    public Film getFilm(Long filmID) {
        return films.get(filmID);
    }


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
        }
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

}