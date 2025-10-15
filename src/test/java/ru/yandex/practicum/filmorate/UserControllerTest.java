package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class UserControllerTest {
    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Test
    void shouldThrowExceptionWhenEmailIsEmpty() {
        User user = new User();
        user.setLogin("validLogin");
        user.setEmail("");  // пустой email
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> userController.addUser(user));
    }

    @Test
    void shouldThrowExceptionWhenEmailHasNoAtSign() {
        User user = new User();
        user.setLogin("validLogin");
        user.setEmail("invalidemail.com");  // нет @
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> userController.addUser(user));
    }

    @Test
    void shouldThrowExceptionWhenLoginIsEmpty() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("");  // пустой login
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> userController.addUser(user));
    }

    @Test
    void shouldThrowExceptionWhenLoginContainsSpace() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("invalid login");  // login с пробелом
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> userController.addUser(user));
    }

    @Test
    void shouldThrowExceptionWhenBirthdayInFuture() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("validLogin");
        user.setBirthday(LocalDate.now().plusDays(1)); // дата в будущем

        assertThrows(ValidationException.class, () -> userController.addUser(user));
    }

    @Test
    void shouldSetNameToLoginWhenNameIsEmpty() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("validLogin");
        user.setName(""); // имя пустое
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = userController.addUser(user);

        assertEquals("validLogin", createdUser.getName(), "Если имя пустое, должно подставляться значение login");
    }

    @Test
    void shouldCreateUserSuccessfully() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("validLogin");
        user.setName("Valid Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userController.addUser(user);

        assertNotNull(createdUser.getId(), "Id должен быть присвоен при создании пользователя");
        assertEquals("valid@email.com", createdUser.getEmail());
        assertEquals("validLogin", createdUser.getLogin());
        assertEquals("Valid Name", createdUser.getName());
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("validLogin");
        user.setName("Old Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userController.addUser(user);

        createdUser.setName("New Name");
        User updatedUser = userController.updateUser(createdUser);

        assertEquals("New Name", updatedUser.getName(), "Имя пользователя должно обновляться");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonexistentUser() {
        User user = new User();
        user.setId(999); // несуществующий ID
        user.setEmail("valid@email.com");
        user.setLogin("validLogin");
        user.setName("Some Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(ValidationException.class, () -> userController.updateUser(user),
                "Ошибка при попытке обновления несуществующего пользователя");
    }


}