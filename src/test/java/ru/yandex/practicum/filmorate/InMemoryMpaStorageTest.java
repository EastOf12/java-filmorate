package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.MpaDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.InMemoryMpaStorage;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({MpaDbStorage.class, InMemoryMpaStorage.class, FilmRowMapper.class})
public class InMemoryMpaStorageTest {
    private final InMemoryMpaStorage inMemoryMpaStorage;


    @Test
    public void shouldReturnPositiveWhenGetAllMpaIsCorrect() {
        Collection<Mpa> allRatings = inMemoryMpaStorage.getAllMpa();
        final Integer MAX_RATING = 5;
        assertEquals(MAX_RATING, allRatings.size());
    } //Проверяем корректность получения всех рейтингов

    @Test
    public void shouldReturnPositiveWhenGetMpaIsCorrect() {

        Mpa mpa = new Mpa();
        mpa.setName("G");
        mpa.setId(1);

        Mpa mpaDtoBd = inMemoryMpaStorage.getMpaBiId(1);

        assertEquals(mpa, mpaDtoBd, "Объекты должны быть равны");
    } //Проверяем корректность получения конкретного рейтинга
}
