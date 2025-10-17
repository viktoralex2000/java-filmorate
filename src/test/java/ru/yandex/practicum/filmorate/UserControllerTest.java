package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnBadRequestWhenEmailIsEmpty() {
        User user = new User();
        user.setLogin("validLogin");
        user.setEmail("");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ResponseEntity<String> response = restTemplate.postForEntity("/users", user, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenEmailHasNoAtSign() {
        User user = new User();
        user.setLogin("validLogin");
        user.setEmail("invalidemail.com");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ResponseEntity<String> response = restTemplate.postForEntity("/users", user, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenLoginIsEmpty() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ResponseEntity<String> response = restTemplate.postForEntity("/users", user, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenLoginContainsSpace() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("invalid login");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ResponseEntity<String> response = restTemplate.postForEntity("/users", user, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenBirthdayInFuture() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("validLogin");
        user.setBirthday(LocalDate.now().plusDays(1));

        ResponseEntity<String> response = restTemplate.postForEntity("/users", user, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldSetNameToLoginWhenNameIsEmpty() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("validLogin");
        user.setName("");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ResponseEntity<User> response = restTemplate.postForEntity("/users", user, User.class);
        User createdUser = response.getBody();
        assertNotNull(createdUser);
        assertEquals("validLogin", createdUser.getName());
    }

    @Test
    void shouldCreateUserSuccessfully() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("validLogin");
        user.setName("Valid Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ResponseEntity<User> response = restTemplate.postForEntity("/users", user, User.class);
        User createdUser = response.getBody();

        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
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

        ResponseEntity<User> createResponse = restTemplate.postForEntity("/users", user, User.class);
        User createdUser = createResponse.getBody();

        assertNotNull(createdUser);
        createdUser.setName("New Name");

        HttpEntity<User> request = new HttpEntity<>(createdUser);
        ResponseEntity<User> updateResponse = restTemplate.exchange("/users",
                org.springframework.http.HttpMethod.PUT, request, User.class);
        User updatedUser = updateResponse.getBody();

        assertNotNull(updatedUser);
        assertEquals("New Name", updatedUser.getName());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonexistentUser() {
        User user = new User();
        user.setId(999);
        user.setEmail("valid@email.com");
        user.setLogin("validLogin");
        user.setName("Some Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        HttpEntity<User> request = new HttpEntity<>(user);
        ResponseEntity<String> response = restTemplate.exchange("/users",
                org.springframework.http.HttpMethod.PUT, request, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
