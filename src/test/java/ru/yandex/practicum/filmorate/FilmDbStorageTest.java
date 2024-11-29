package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.*;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmRowMapper.class, FilmDbStorage.class, UserDbStorage.class,
        UserRowMapper.class, UserFriendDbStorage.class, LikesDbStorage.class,
        GenreDbStorage.class, MpaDbStorage.class})


public class FilmDbStorageTest {
    private final MpaDbStorage mpaDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final UserDbStorage userDbStorage;
    private Film film;

    @BeforeEach
    public void beforeEachFile() throws IOException {

        //Создаем объект фильма с правильными параметрами.
        film = new Film();
        film.setName("Человек паук");
        film.setDescription("Человека укусил паук и тот стал супер героем.");
        film.setReleaseDate(LocalDate.of(2004, 12, 12));
        film.setDuration(1000);

        Collection<Genre> genres = new HashSet<>();
        film.setGenres(genres);

    }


    @Test
    public void shouldReturnPositiveWhenCreateFilmIsCorrect() {
        //Добавляем фильм
        filmDbStorage.createFilm(film);

        //Проверяем, что фильм с правильными успешно добавлен.
        assertEquals(1, filmDbStorage.findAll().size(), "Должен быть 1 фильм");

    } //Проверяем корректность добавления фильма с правильными параметрами.

    @Test
    public void shouldReturnPositiveWhenUpdateFilmIsCorrect() {

        //Добавляем фильм
        Film cratedFilm = filmDbStorage.createFilm(film);
        assertEquals(1, filmDbStorage.findAll().size());

        //Создаем объект фильма для обновления.
        Film filmNew = new Film();
        filmNew.setName("Новый человек паук");
        filmNew.setDescription("Человека укусил паук и тот стал супер героем.");
        filmNew.setReleaseDate(LocalDate.of(2024, 12, 12));
        filmNew.setDuration(1000);
        filmNew.setId(cratedFilm.getId());
        Mpa mpa = new Mpa();
        mpa.setId(1);
        mpa.setName("G");
        filmNew.setMpa(mpa);

        Collection<Genre> genres = new HashSet<>();
        filmNew.setGenres(genres);

        //Обновляем фильм
        filmDbStorage.updateFilm(filmNew);

        //Проверяем что фильм по прежнему 1.
        assertEquals(1, filmDbStorage.findAll().size());

        //Проверяем, что фильм обновлен.
        assertNotEquals(filmNew, filmDbStorage.findById(film.getId()),
                "Фильмы не должны быть одинаковыми.");
    } //Проверяем корректность обновления фильма с правильными параметрами.

    @Test
    public void shouldReturnPositiveWhenGetAllIsCorrect() {
        //Проверяем что фильмы еще не добавлялись.
        assertEquals(0, filmDbStorage.findAll().size(), "Не должно быть фильмов");

        //Добавляем фильм
        filmDbStorage.createFilm(film);

        //Проверяем, что в ответе метода есть добавленный фильм
        assertEquals(1, filmDbStorage.findAll().size(), "Должен быть 1 фильм");
    }

    @Test
    public void shouldReturnPositiveWhenGetAllMpaIsCorrect() {
        Collection<Mpa> allRatings = mpaDbStorage.getAllMpa();
        final Integer MAX_RATING = 5;
        assertEquals(MAX_RATING, allRatings.size());
    } //Проверяем корректность получения всех рейтингов

    @Test
    public void shouldReturnPositiveWhenGetMpaIsCorrect() {

        Mpa mpa = new Mpa();
        mpa.setName("G");
        mpa.setId(1);

        Mpa mpaDtoBd = mpaDbStorage.getMpa(1);

        assertEquals(mpa, mpaDtoBd, "Объекты должны быть равны");
    } //Проверяем корректность получения конкретного рейтинга

    @Test
    public void shouldReturnPositiveWhenGetAllGenresIsCorrect() {

        Collection<Genre> allGenres = genreDbStorage.getAllGenres();
        final Integer MAX_GENRE = 6;
        assertEquals(MAX_GENRE, allGenres.size());
    } //Проверяем корректность получения всех жанров

    @Test
    public void shouldReturnPositiveWhenGetGenreIsCorrect() {
        Genre genre = new Genre();
        genre.setName("Комедия");
        genre.setId(1);

        Genre genreBd = genreDbStorage.getGenre(1);

        assertEquals(genre, genreBd, "Объекты должны быть равны");

    } //Проверяем корректность получения конкретного жанра
}
