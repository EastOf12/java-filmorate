package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class FilmService {
    private final Map<Long, Film> films = new HashMap<>();
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

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

    public Film update(Film film) {
        log.trace("Получен запрос на обновление информации по фильму");

        //Проверяем корректность переданного ID
        if (film.getId() == null) {
            log.warn("Валидация не пройдена. Id должен быть указан");
            throw new ConditionsNotMetException("Id должен быть указан");
        }

        if (!films.containsKey(film.getId())) {
            log.warn("Валидация не пройдена. Фильм с id = {} не найден", film.getId());
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }

        //Обновляем информацию по фильму в памяти приложения
        if (film.getName() != null && !film.getName().isEmpty()) {
            films.get(film.getId()).setName(film.getName());
        }

        if (film.getDescription() != null && !film.getDescription().isEmpty() && film.getDescription().length() <= 200) {
            films.get(film.getId()).setDescription(film.getDescription());
        }

        if (film.getReleaseDate() != null && !film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            films.get(film.getId()).setReleaseDate(film.getReleaseDate());
        }

        if (film.getDuration() < 0) {
            films.get(film.getId()).setDuration(film.getDuration());
        }

        log.info("Обновлена информация по фильму {}", film.getId());
        return film;
    }

    public Collection<Film> getAll() {
        log.info("Отправили информацию по все фильмам.");
        return films.values();
    }

    private void passValidationCreate(Film film) {
        //Проверяем корректность заполнения полей.
        if (film.getName() == null || film.getName().isEmpty()) {
            log.warn("Валидация не пройдена. Не было передано название фильма.");
            throw new ValidationException("Название не может быть пустым.");
        } else if (film.getDescription() == null || film.getDescription().isEmpty()) {
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
        } else if (film.getDuration() < 0) {
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
