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

    @Test
    void shouldAddFriendSuccessfully() {
        User user1 = createUser("user1@email.com", "user1");
        User user2 = createUser("user2@email.com", "user2");

        restTemplate.put("/users/{id}/friends/{friendId}", null, user1.getId(), user2.getId());

        ResponseEntity<User> response1 = restTemplate.getForEntity("/users/{id}", User.class, user1.getId());
        ResponseEntity<User> response2 = restTemplate.getForEntity("/users/{id}", User.class, user2.getId());

        assertTrue(response1.getBody().getFriends().contains(user2.getId()));
        assertTrue(response2.getBody().getFriends().contains(user1.getId()));
    }

    @Test
    void shouldRemoveFriendSuccessfully() {
        User user1 = createUser("user1@email.com", "user1");
        User user2 = createUser("user2@email.com", "user2");

        restTemplate.put("/users/{id}/friends/{friendId}", null, user1.getId(), user2.getId());
        restTemplate.delete("/users/{id}/friends/{friendId}", user1.getId(), user2.getId());

        ResponseEntity<User> response1 = restTemplate.getForEntity("/users/{id}", User.class, user1.getId());
        ResponseEntity<User> response2 = restTemplate.getForEntity("/users/{id}", User.class, user2.getId());

        assertFalse(response1.getBody().getFriends().contains(user2.getId()));
        assertFalse(response2.getBody().getFriends().contains(user1.getId()));
    }

    @Test
    void shouldReturnListOfFriends() {
        User user1 = createUser("user1@email.com", "user1");
        User user2 = createUser("user2@email.com", "user2");
        User user3 = createUser("user3@email.com", "user3");

        restTemplate.put("/users/{id}/friends/{friendId}", null, user1.getId(), user2.getId());
        restTemplate.put("/users/{id}/friends/{friendId}", null, user1.getId(), user3.getId());

        ResponseEntity<User[]> response = restTemplate.getForEntity("/users/{id}/friends", User[].class, user1.getId());

        assertEquals(2, response.getBody().length);
    }

    @Test
    void shouldReturnCommonFriends() {
        User user1 = createUser("user1@email.com", "user1");
        User user2 = createUser("user2@email.com", "user2");
        User user3 = createUser("user3@email.com", "user3");

        // все друзья user1 и user2 добавляют user3 в друзья
        restTemplate.put("/users/{id}/friends/{friendId}", null, user1.getId(), user3.getId());
        restTemplate.put("/users/{id}/friends/{friendId}", null, user2.getId(), user3.getId());

        ResponseEntity<User[]> response = restTemplate.getForEntity("/users/{id}/friends/common/{otherId}",
                User[].class, user1.getId(), user2.getId());

        assertEquals(1, response.getBody().length);
        assertEquals(user3.getId(), response.getBody()[0].getId());
    }

    // Вспомогательный метод для создания пользователя
    private User createUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return restTemplate.postForEntity("/users", user, User.class).getBody();
    }

    @Test
    void shouldReturnNotFoundWhenAddingFriendToNonexistentUser() {
        User user = createUser("user@email.com", "user");

        ResponseEntity<String> response = restTemplate.exchange(
                "/users/{id}/friends/{friendId}",
                HttpMethod.PUT, null, String.class, 999L, user.getId());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenRemovingFriendFromNonexistentUser() {
        User user = createUser("user@email.com", "user");

        ResponseEntity<String> response = restTemplate.exchange(
                "/users/{id}/friends/{friendId}",
                HttpMethod.DELETE, null, String.class, 999L, user.getId());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

}
