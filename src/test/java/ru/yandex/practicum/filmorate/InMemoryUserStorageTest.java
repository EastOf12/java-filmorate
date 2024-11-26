package ru.yandex.practicum.filmorate;


import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dal.UserFriendDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserRowMapper.class, UserDbStorage.class, InMemoryUserStorage.class
        , UserRowMapper.class, UserFriendDbStorage.class})
public class InMemoryUserStorageTest {

    private final InMemoryUserStorage inMemoryUserStorage;
    private User user;

    @BeforeEach
    public void beforeEachFile() throws IOException {

        //Создаем объект фильма с правильными параметрами.
        user = new User();
        user.setLogin("Bob123");
        user.setName("Bob");
        user.setEmail("blabla@gmail.com");
        user.setBirthday(LocalDate.of(2000, 12, 12));
    }

    @Test
    public void shouldReturnPositiveWhenCreateUserIsCorrect() {
        //Добавляем пользователя
        inMemoryUserStorage.create(user);

        //Проверяем, что пользователь с правильными параметрами успешно добавлен.
        assertEquals(1, inMemoryUserStorage.getAll().size(), "Пользователь не создан");
        assertEquals(user, inMemoryUserStorage.getAll().stream().findFirst().orElse(null), "Пользователи не " +
                "равны");
    } //Проверяем корректность добавления пользователя с правильными параметрами.


    @Test
    public void shouldReturnPositiveWhenUpdateUserIsCorrect() {
        //Добавляем пользователя
        inMemoryUserStorage.create(user);
        assertEquals(1, inMemoryUserStorage.getAll().size(), "Пользователь должен быть 1");

        //Создаем объект пользователя с правильными параметрами для обновления.
        User newUser = new User();
        newUser.setEmail("blabla@gmail.com");
        newUser.setName("Boris");
        newUser.setLogin("bor12345");
        newUser.setBirthday(LocalDate.of(2004, 12, 12));
        newUser.setId(user.getId());

        //Обновляем пользователя
        inMemoryUserStorage.update(newUser);

        //Проверяем что пользователь по прежнему 1.
        assertEquals(1, inMemoryUserStorage.getAll().size(), "Пользователь должен быть 1");

        //Проверяем, что пользователь обновлен.
        assertEquals(newUser, Objects.requireNonNull(inMemoryUserStorage.getAll().stream().findFirst()
                .orElse(null)), "Пользователи не равны");
    } //Проверяем корректность обновления пользователя с правильными параметрами.

    @Test
    public void shouldReturnPositiveWhenNameValidationIsCorrect() {
        //Делаем имя пользователя пустым и проверяем валидацию.
        user.setName("");
        inMemoryUserStorage.create(user);

        //Проверяем, что пользователь создан
        assertEquals(1, inMemoryUserStorage.getAll().size(), "Пользователь должен быть 1");
        assertEquals(user.getName(), Objects.requireNonNull(inMemoryUserStorage.getAll().stream().findFirst()
                        .orElse(null)).getLogin(),
                "Имя пользователя не равно логину.");
    } //Проверяем корректность работы валидации на название фильма

    @Test
    public void shouldReturnPositiveWhenMailValidationIsCorrect() {
        //Делаем email пользователя некорректным и проверяем валидацию.
        user.setEmail(null);
        assertThrows(ValidationException.class, () -> inMemoryUserStorage.create(user),
                "Не выброшено исключение ValidationException");

        user.setEmail(" ");
        assertThrows(ValidationException.class, () -> inMemoryUserStorage.create(user),
                "Не выброшено исключение ValidationException");

        user.setEmail("12313gmail");
        assertThrows(ValidationException.class, () -> inMemoryUserStorage.create(user),
                "Не выброшено исключение ValidationException");

        assertEquals(0, inMemoryUserStorage.getAll().size(), "Не должно быть добавленных пользователей.");
    } //Проверяем корректность работы валидации на email от пользователя

    @Test
    public void shouldReturnPositiveWhenLoginValidationIsCorrect() {
        //Делаем логин пользователя некорректным и проверяем валидацию.
        user.setLogin(null);
        assertThrows(ValidationException.class, () -> inMemoryUserStorage.create(user),
                "Не выброшено исключение ValidationException");

        user.setLogin(" ");
        assertThrows(ValidationException.class, () -> inMemoryUserStorage.create(user),
                "Не выброшено исключение ValidationException");

        user.setLogin("my login");
        assertThrows(ValidationException.class, () -> inMemoryUserStorage.create(user),
                "Не выброшено исключение ValidationException");
        assertEquals(0, inMemoryUserStorage.getAll().size(), "Не должно быть добавленных пользователей.");
    } //Проверяем корректность работы валидации на логин пользователя

    @Test
    public void shouldReturnPositiveWhenBirthdayValidationIsCorrect() {
        //Делаем дату рождения некорректной и проверяем валидацию.
        user.setBirthday(null);
        assertThrows(ValidationException.class, () -> inMemoryUserStorage.create(user),
                "Не выброшено исключение ValidationException");

        user.setBirthday(LocalDate.now().plusDays(1));
        assertThrows(ValidationException.class, () -> inMemoryUserStorage.create(user),
                "Не выброшено исключение ValidationException");

        assertEquals(0, inMemoryUserStorage.getAll().size(), "Не должно быть добавленных пользователей.");
    } //Проверяем корректность работы валидации на длительность фильма

    @Test
    public void shouldReturnPositiveWhenGetAllIsCorrect() {
        //Проверяем что пользователи еще не добавлялись.
        assertEquals(0, inMemoryUserStorage.getAll().size(), "Не должно быть пользователей.");

        //Добавляем пользователя
        inMemoryUserStorage.create(user);

        //Проверяем, что в ответе метода есть добавленный пользователь
        assertTrue(inMemoryUserStorage.getAll().contains(user));
    }
}
