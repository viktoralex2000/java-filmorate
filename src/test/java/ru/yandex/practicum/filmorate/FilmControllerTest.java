package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FilmControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    @Test
    void shouldReturnBadRequestWhenNameIsEmpty() {
        Film film = new Film();
        film.setName("");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        ResponseEntity<String> response = restTemplate.postForEntity("/films", film, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionTooLong() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("A".repeat(201));
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        ResponseEntity<String> response = restTemplate.postForEntity("/films", film, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldCreateFilmEvenIfReleaseDateBeforeCinemaBirthday() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(1800, 1, 1)); // дата до 28.12.1895
        film.setDuration(100);

        ResponseEntity<Film> response = restTemplate.postForEntity("/films", film, Film.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getId(), "ID фильма должен быть присвоен");
        assertEquals(LocalDate.of(1800, 1, 1), response.getBody().getReleaseDate(), "Дата релиза должна сохраняться");
    }


    @Test
    void shouldCreateFilmOnCinemaBirthday() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(CINEMA_BIRTHDAY);
        film.setDuration(100);

        ResponseEntity<Film> response = restTemplate.postForEntity("/films", film, Film.class);
        Film createdFilm = response.getBody();

        assertNotNull(createdFilm);
        assertEquals(CINEMA_BIRTHDAY, createdFilm.getReleaseDate());
    }

    @Test
    void shouldReturnBadRequestWhenDurationIsZeroOrNegative() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0);

        ResponseEntity<String> response = restTemplate.postForEntity("/films", film, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldCreateFilmSuccessfully() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        ResponseEntity<Film> response = restTemplate.postForEntity("/films", film, Film.class);
        Film createdFilm = response.getBody();

        assertNotNull(createdFilm);
        assertNotNull(createdFilm.getId());
        assertEquals("Фильм", createdFilm.getName());
        assertEquals("Описание", createdFilm.getDescription());
    }

    @Test
    void shouldUpdateFilmSuccessfully() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        ResponseEntity<Film> createResponse = restTemplate.postForEntity("/films", film, Film.class);
        Film createdFilm = createResponse.getBody();
        assertNotNull(createdFilm);

        createdFilm.setDescription("Новое описание");
        HttpEntity<Film> request = new HttpEntity<>(createdFilm);
        ResponseEntity<Film> updateResponse = restTemplate.exchange("/films",
                org.springframework.http.HttpMethod.PUT, request, Film.class);
        Film updatedFilm = updateResponse.getBody();

        assertNotNull(updatedFilm);
        assertEquals("Новое описание", updatedFilm.getDescription());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonexistentFilm() {
        Film film = new Film();
        film.setId(999);
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        HttpEntity<Film> request = new HttpEntity<>(film);
        ResponseEntity<String> response = restTemplate.exchange("/films",
                org.springframework.http.HttpMethod.PUT, request, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
