package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnBadRequestWhenEmailIsEmpty() {
        User user = new User();
        user.setLogin("validLogin");
        user.setEmail(""); // пустой email
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ResponseEntity<Map> response = restTemplate.postForEntity("/users", user, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("error").toString().contains("Email не может быть пустым"));
    }

    @Test
    void shouldReturnBadRequestWhenEmailHasNoAtSign() {
        User user = new User();
        user.setLogin("validLogin");
        user.setEmail("invalidemail.com"); // некорректный формат
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ResponseEntity<Map> response = restTemplate.postForEntity("/users", user, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("error").toString().contains("Некорректный формат email"));
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
    void shouldReturnNotFoundWhenUpdatingNonexistentUser() {
        User user = new User();
        user.setId(999);
        user.setEmail("valid@email.com");
        user.setLogin("validLogin");
        user.setName("Some Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        HttpEntity<User> request = new HttpEntity<>(user);
        ResponseEntity<String> response = restTemplate.exchange("/users",
                HttpMethod.PUT, request, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
