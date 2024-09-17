package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;


@Service
@Slf4j
public class FilmService {
    @Autowired
    private FilmStorage filmStorage;

    @Autowired
    private UserStorage userStorage;


    public void addLike(Long filmId, Long userId) {
        Film film = filmStorage.getFilm(filmId);

        //Находим фильм по его айди
        if (filmStorage.checkFilmAvailability(filmId)) {
            log.warn("Нет фильма с id {}", filmId);
            throw new NotFoundException("Нет фильма с id " + filmId);
        }

        //Находим пользователя с таким айди
        if (userStorage.checkUserAvailability(userId)) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        //Добавляем лайк на фильм
        if (film.getLikes().contains(userId)) {
            log.warn("Лайк к фильму с id {} был добавлен ранее", film.getId());
            throw new ValidationException("Лайк был добавлен ранее");
        } else {
            log.info("Поставили лайк фильму с id={}", film.getId());
            film.getLikes().add(userId);
        }
    } //Добавляет лайк

    public void deleteLike(Long filmId, Long userId) {
        //Находим фильм по его айди
        Film film = filmStorage.getFilm(filmId);

        //Находим фильм по его айди
        if (filmStorage.checkFilmAvailability(filmId)) {
            log.warn("Нет фильма с id {}", filmId);
            throw new NotFoundException("Нет фильма с id " + filmId);
        }

        //Находим пользователя с таким айди
        if (userStorage.checkUserAvailability(userId)) {
            log.warn("Нет пользователя с id {}", userId);
            throw new NotFoundException("Нет пользователя с id " + userId);
        }

        //Удаляем лайк с фильма
        if (film.getLikes().contains(userId)) {
            log.warn("Удалили лайк с фильма {}", filmId);
            film.getLikes().remove(userId);

        } else {
            log.info("Пользователь {} не ставил лайк фильму id={}", userId, filmId);
            throw new ValidationException("Пользователь не ставил лайк фильму " + filmId);
        }

    } //Удаляет лайк

    public Collection<Film> getPopularFilms(int count) {
        //Получаем все доступные фильмы.
        Collection<Film> films = filmStorage.getAll();

        if (films == null) {
            log.info("Фильмы не найдены");
            return new ArrayList<>();
        } else {
            //Сортируем фильмы по количеству лайков
            Comparator<Film> likesComparator = new Comparator<Film>() {
                @Override
                public int compare(Film film1, Film film2) {
                    int likesCount1 = film1.getLikes().size();
                    int likesCount2 = film2.getLikes().size();
                    return Integer.compare(likesCount2, likesCount1);
                }
            };

            // Сортировка коллекции films
            List<Film> sortedFilms = new ArrayList<>(films);
            sortedFilms.sort(likesComparator);

            // Оставляем нужное количество фильмов
            log.info("Вернули топ популярных фильмов в колличестве {}", count);
            return sortedFilms.subList(0, Math.min(count, sortedFilms.size()));
        }
    } //Возвращает самые популярные фильмы
}
