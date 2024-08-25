package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class FilmServiceTest {
    Film film = new Film();
    private FilmService filmService;

    @BeforeEach
    public void beforeEachFile() throws IOException {
        filmService = new FilmService();

        //Создаем объект фильма с правильными параметрами.
        film = new Film();
        film.setName("Человек паук");
        film.setDescription("Человека укусил паук и тот стал супер героем.");
        film.setReleaseDate(LocalDate.of(2004, 12, 12));
        film.setDuration(1000);
    }

    @Test
    public void shouldReturnPositiveWhenCreateFilmIsCorrect() {
        //Добавляем фильм
        filmService.create(film);

        //Проверяем, что фильм с правильными параметрыми успешно добавлен.
        assertEquals(1, filmService.getAll().size(), "Должен быть 1 фильм");
        assertEquals(film, filmService.getAll().stream().findFirst().orElse(null),
                "Фильмы должны быть равны");
    } //Проверяем корректность добавления фильма с правильными параметрами.

    @Test
    public void shouldReturnPositiveWhenUpdateFilmIsCorrect() {
        //Добавляем фильм
        filmService.create(film);
        assertEquals(1, Objects.requireNonNull(filmService.getAll().stream().findFirst().orElse(null))
                        .getId());

        //Создаем объект фильма для обновления.
        Film filmNew = new Film();
        filmNew.setName("Новый человек паук");
        filmNew.setDescription("Человека укусил паук и тот стал супер героем.");
        filmNew.setReleaseDate(LocalDate.of(2024, 12, 12));
        filmNew.setDuration(1000);
        filmNew.setId((long) 1);

        //Обновляем фильм
        filmService.update(filmNew);

        //Проверяем что фильм по прежнему 1.
        assertEquals(1, Objects.requireNonNull(filmService.getAll().stream().findFirst().orElse(null))
                        .getId());

        //Проверяем, что фильм обновлен.
        assertEquals(filmNew, filmService.getAll().stream().findFirst().orElse(null),
                "Фильмы должны быть одинаковыми.");
    } //Проверяем корректность обновления фильма с правильными параметрами.

    @Test
    public void shouldReturnPositiveWhenNameValidationIsCorrect() {
        //Делаем название фильма некорректным и проверяем валидацию.
        film.setName("");
        assertThrows(ValidationException.class, () -> filmService.create(film),
                "Не выброшено исключение ValidationException");
        film.setName(null);
        assertThrows(ValidationException.class, () -> filmService.create(film),
                "Не выброшено исключение ValidationException");

        //Проверяем, что фильм по прежнему не создан
        assertEquals(0, filmService.getAll().size(), "Фильм не должен быть создан");
    } //Проверяем корректность работы валидации на название фильма

    @Test
    public void shouldReturnPositiveWhenDescriptionValidationIsCorrect() {
        //Делаем описание фильма некорректным и проверяем валидацию.
        film.setDescription(stringGenerate(201));
        assertThrows(ValidationException.class, () -> filmService.create(film),
                "Не выброшено исключение ValidationException");

        assertEquals(0, filmService.getAll().size(), "Фильм не должен быть создан");

        //Создаем фильм с максимально возможным количеством символов в описании.
        film.setDescription(stringGenerate(200));
        filmService.create(film);
        assertEquals(1, filmService.getAll().size(), "Фильм должен быть создан");
    } //Проверяем корректность работы валидации на описание фильма

    @Test
    public void shouldReturnPositiveWhenReleaseDateValidationIsCorrect() {
        //Делаем дату выхода фильма некорректной и проверяем валидацию.
        film.setReleaseDate(LocalDate.from(LocalDate.of(1895, 12, 27)));

        assertThrows(ValidationException.class, () -> filmService.create(film),
                "Не выброшено исключение ValidationException");
        assertEquals(0, filmService.getAll().size(), "Фильм не должен быть создан");

        //Проверяем корректность самой ранней даты выхода фильма
        film.setReleaseDate(LocalDate.from(LocalDate.of(1895, 12, 28)));
        filmService.create(film);
        assertEquals(1, filmService.getAll().size(), "Фильм должен быть создан");
    } //Проверяем корректность работы валидации на дату релиза фильма

    @Test
    public void shouldReturnPositiveWhenDurationValidationIsCorrect() {
        //Делаем длительность фильма некорректной и проверяем валидацию.
        film.setDuration(-1);
        assertThrows(ValidationException.class, () -> filmService.create(film),
                "Не выброшено исключение ValidationException");

        //Проверяем корректность с минимальной длительностью фильма
        film.setDuration(0);
        filmService.create(film);
        assertEquals(1, filmService.getAll().size(), "Фильм должен быть создан");
    } //Проверяем корректность работы валидации на длительность фильма

    @Test
    public void shouldReturnPositiveWhenGetAllIsCorrect() {
        //Проверяем что фильмы еще не добавлялись.
        assertEquals(0, filmService.getAll().size(), "Не должно быть фильмов");

        //Добавляем фильм
        filmService.create(film);

        //Проверяем, что в ответе метода есть добавленный фильм
        assertTrue(filmService.getAll().contains(film));
    }

    private String stringGenerate(int desiredLength) {
        StringBuilder sb = new StringBuilder(desiredLength);

        while (sb.length() < desiredLength) {
            sb.append("a");
        }

        return sb.toString();
    }
}
