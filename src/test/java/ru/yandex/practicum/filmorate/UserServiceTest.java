package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
    User user = new User();
    private UserService userService;

    @BeforeEach
    public void beforeEachFile() throws IOException {
        userService = new UserService();

        //Создаем объект пользователя с правильными параметрами.
        user = new User();
        user.setEmail("blabla@gmail.com");
        user.setName("Boris");
        user.setLogin("bor12345");
        user.setBirthday(LocalDate.of(2004, 12, 12));
    }

    @Test
    public void shouldReturnPositiveWhenCreateUserIsCorrect() {
        //Добавляем пользователя
        userService.create(user);

        //Проверяем, что пользователь с правильными параметрыми успешно добавлен.
        assertEquals(1, userService.getAll().size(), "Пользователь не создан");
        assertEquals(user, userService.getAll().stream().findFirst().orElse(null), "Пользователи не " +
                "равны");
    } //Проверяем корректность добавления пользователя с правильными параметрами.

    @Test
    public void shouldReturnPositiveWhenUpdateUserIsCorrect() {
        //Добавляем пользователя
        userService.create(user);
        assertEquals(1, Objects.requireNonNull(userService.getAll().stream().findFirst()
                        .orElse(null)).getId(), "Пользователь должен быть 1");

        //Создаем объект пользователя с правильными параметрами для обновления.
        User newUser = new User();
        newUser.setEmail("blabla@gmail.com");
        newUser.setName("Boris");
        newUser.setLogin("bor12345");
        newUser.setBirthday(LocalDate.of(2004, 12, 12));
        newUser.setId((long) 1);

        //Обновляем пользователя
        userService.update(newUser);

        //Проверяем что пользователь по прежнему 1.
        assertEquals(1, Objects.requireNonNull(userService.getAll().stream().findFirst()
                        .orElse(null)).getId(), "Пользователь должен быть 1");

        //Проверяем, что пользователь обновлен.
        assertEquals(newUser, Objects.requireNonNull(userService.getAll().stream().findFirst()
                        .orElse(null)), "Пользователи не равны");
    } //Проверяем корректность обновления пользователя с правильными параметрами.

    @Test
    public void shouldReturnPositiveWhenNameValidationIsCorrect() {
        //Делаем имя пользователя пустым и проверяем валидацию.
        user.setName("");
        userService.create(user);

        //Проверяем, что пользователь создан
        assertEquals(1, userService.getAll().size(), "Пользователь должен быть 1");
        assertEquals(user.getName(), Objects.requireNonNull(userService.getAll().stream().findFirst()
                                .orElse(null)).getLogin(),
                "Имя пользователя не равно логину.");
    } //Проверяем корректность работы валидации на название фильма

    @Test
    public void shouldReturnPositiveWhenMailValidationIsCorrect() {
        //Делаем email пользователя некорректным и проверяем валидацию.
        user.setEmail(null);
        assertThrows(ValidationException.class, () -> userService.create(user),
                "Не выброшено исключение ValidationException");

        user.setEmail(" ");
        assertThrows(ValidationException.class, () -> userService.create(user),
                "Не выброшено исключение ValidationException");

        user.setEmail("12313gmail");
        assertThrows(ValidationException.class, () -> userService.create(user),
                "Не выброшено исключение ValidationException");

        assertEquals(0, userService.getAll().size(), "Не должно быть добавленных пользователей.");
    } //Проверяем корректность работы валидации на email от пользователя

    @Test
    public void shouldReturnPositiveWhenLoginValidationIsCorrect() {
        //Делаем логин пользователя некорректным и проверяем валидацию.
        user.setLogin(null);
        assertThrows(ValidationException.class, () -> userService.create(user),
                "Не выброшено исключение ValidationException");

        user.setLogin(" ");
        assertThrows(ValidationException.class, () -> userService.create(user),
                "Не выброшено исключение ValidationException");

        user.setLogin("my login");
        assertThrows(ValidationException.class, () -> userService.create(user),
                "Не выброшено исключение ValidationException");
        assertEquals(0, userService.getAll().size(), "Не должно быть добавленных пользователей.");
    } //Проверяем корректность работы валидации на логин пользователя

    @Test
    public void shouldReturnPositiveWhenBirthdayValidationIsCorrect() {
        //Делаем дату рождения некорректной и проверяем валидацию.
        user.setBirthday(null);
        assertThrows(ValidationException.class, () -> userService.create(user),
                "Не выброшено исключение ValidationException");

        user.setBirthday(LocalDate.now().plusDays(1));
        assertThrows(ValidationException.class, () -> userService.create(user),
                "Не выброшено исключение ValidationException");

        assertEquals(0, userService.getAll().size(), "Не должно быть добавленных пользователей.");
    } //Проверяем корректность работы валидации на длительность фильма

    @Test
    public void shouldReturnPositiveWhenGetAllIsCorrect() {
        //Проверяем что пользователи еще не добавлялись.
        assertEquals(0, userService.getAll().size(), "Не должно быть пользователей.");

        //Добавляем пользователя
        userService.create(user);

        //Проверяем, что в ответе метода есть добавленный пользователь
        assertTrue(userService.getAll().contains(user));
    }
}
