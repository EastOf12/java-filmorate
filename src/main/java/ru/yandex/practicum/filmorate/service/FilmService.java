package ru.yandex.practicum.filmorate.service;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Getter
@Service
public class FilmService {
    private static final Logger log = LoggerFactory.getLogger(FilmService.class);

    private final Map<Long, Film> films = new HashMap<>();

    public Film create(Film film) {
        log.trace("Получен запрос на добавление нового фильма");

        //Проходим валидацию полей.
        passValidation(film);
        log.debug("Валидация пройдена.");

        //Сохраняем новый фильм в памяти приложения
        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Добавлен новый фильм " + film.getId());
        return film;
    }

    public Film update(Film film) {
        log.trace("Получен запрос на обновление информации по фильму");

        //Проверяем корректность переданного ID
        if (film.getId() == null) {
            log.warn("Валидация не пройдена. Id должен быть указан");
            throw new ConditionsNotMetException("Id должен быть указан");
        }

        if (!films.containsKey(film.getId())) {
            log.warn("Валидация не пройдена. Фильм с id = " + film.getId() + " не найден");
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }

        //Проходим валидацию полей.
        passValidation(film);
        log.debug("Валидация пройдена.");

        //Обновляем информацию по фильму в памяти приложения
        films.put(film.getId(), film);
        log.info("Обновлена информация по фильму " + film.getId());
        return film;
    }

    public Collection<Film> getAll() {
        log.info("Отправили информацию по все фильмам.");
        return films.values();
    }

    private void passValidation(Film film) {
        //Проверяем корректность заполнения полей.
        if (film.getName() == null || film.getName().isEmpty()) {
            log.warn("Валидация не пройдена. Не было передано название фильма.");

            throw new ValidationException("Название не может быть пустым.");
        } else if (film.getDescription().length() > 200) {
            log.warn("Валидация не пройдена. Количество символов в описании запроса "
                    + film.getDescription().length());

            throw new ValidationException("Максимальная длина описания — 200 символов.");
        } else if (film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            log.warn("Валидация не пройдена. Дата в запросе "
                    + film.getReleaseDate());

            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года.");
        } else if (film.getDuration() < 0) {
            log.warn("Валидация не пройдена. " +
                    "Продолжительность фильма в запросе " + film.getDuration());

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
