package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FilmLikesControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private Film film;
    private User user;

    @BeforeEach
    void setup() {
        // Создаём пользователя
        user = new User();
        user.setEmail("testuser@email.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        ResponseEntity<User> userResponse = restTemplate.postForEntity("/users", user, User.class);
        user = userResponse.getBody();
        assertNotNull(user);
        assertNotNull(user.getId());

        // Создаём фильм
        film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        ResponseEntity<Film> filmResponse = restTemplate.postForEntity("/films", film, Film.class);
        film = filmResponse.getBody();
        assertNotNull(film);
        assertNotNull(film.getId());
    }

    @Test
    void shouldAddLikeToFilm() {
        restTemplate.put("/films/{id}/like/{userId}", null, film.getId(), user.getId());

        ResponseEntity<Film> response = restTemplate.getForEntity("/films/{id}", Film.class, film.getId());
        Film updatedFilm = response.getBody();
        assertNotNull(updatedFilm);
        assertTrue(updatedFilm.getLikes().contains(user.getId()), "Лайк пользователя должен быть добавлен");
    }

    @Test
    void shouldRemoveLikeFromFilm() {
        // сначала добавляем лайк
        restTemplate.put("/films/{id}/like/{userId}", null, film.getId(), user.getId());

        // потом удаляем
        restTemplate.delete("/films/{id}/like/{userId}", film.getId(), user.getId());

        ResponseEntity<Film> response = restTemplate.getForEntity("/films/{id}", Film.class, film.getId());
        Film updatedFilm = response.getBody();
        assertNotNull(updatedFilm);
        assertFalse(updatedFilm.getLikes().contains(user.getId()), "Лайк пользователя должен быть удалён");
    }

    @Test
    void shouldReturnPopularFilmsSortedByLikes() {
        // создаём ещё один фильм
        Film film2 = new Film();
        film2.setName("Second Film");
        film2.setDescription("Second Description");
        film2.setReleaseDate(LocalDate.of(2001, 1, 1));
        film2.setDuration(100);
        ResponseEntity<Film> film2Response = restTemplate.postForEntity("/films", film2, Film.class);
        film2 = film2Response.getBody();
        assertNotNull(film2);

        // Добавляем лайки: первый фильм 1 лайк, второй фильм 2 лайка
        restTemplate.put("/films/{id}/like/{userId}", null, film.getId(), user.getId());

        User user2 = new User();
        user2.setEmail("seconduser@email.com");
        user2.setLogin("seconduser");
        user2.setName("Second User");
        user2.setBirthday(LocalDate.of(1991, 2, 2));
        user2 = restTemplate.postForEntity("/users", user2, User.class).getBody();
        assertNotNull(user2);

        restTemplate.put("/films/{id}/like/{userId}", null, film2.getId(), user.getId());
        restTemplate.put("/films/{id}/like/{userId}", null, film2.getId(), user2.getId());

        // Запрос популярных фильмов
        ResponseEntity<Film[]> response = restTemplate.getForEntity("/films/popular?count=2", Film[].class);
        Film[] popularFilms = response.getBody();
        assertNotNull(popularFilms);
        assertEquals(2, popularFilms.length);
        // Фильм с большим количеством лайков должен быть первым
        assertEquals(film2.getId(), popularFilms[0].getId());
        assertEquals(film.getId(), popularFilms[1].getId());
    }

    @Test
    void shouldReturnNotFoundWhenAddingLikeToNonexistentFilm() {
        long fakeFilmId = 9999L;
        ResponseEntity<String> response = restTemplate.exchange(
                "/films/{id}/like/{userId}", org.springframework.http.HttpMethod.PUT,
                null, String.class, fakeFilmId, user.getId());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenRemovingLikeFromNonexistentFilm() {
        long fakeFilmId = 9999L;
        ResponseEntity<String> response = restTemplate.exchange(
                "/films/{id}/like/{userId}", org.springframework.http.HttpMethod.DELETE,
                null, String.class, fakeFilmId, user.getId());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
