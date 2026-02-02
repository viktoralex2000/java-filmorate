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
import java.util.Set;

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
        film.setMpa(1);
        film.setGenreIds(Set.of(1));

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
        film.setMpa(1);
        film.setGenreIds(Set.of(1));

        ResponseEntity<String> response = restTemplate.postForEntity("/films", film, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenReleaseDateBeforeCinemaBirthday() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(1800, 1, 1)); // до 28.12.1895
        film.setDuration(100);
        film.setMpa(1);
        film.setGenreIds(Set.of(1));

        ResponseEntity<String> response = restTemplate.postForEntity("/films", film, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "При дате релиза до 28.12.1895 должен возвращаться 400 Bad Request");
    }

    @Test
    void shouldReturnBadRequestWhenDurationIsZeroOrNegative() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0);
        film.setMpa(1);
        film.setGenreIds(Set.of(1));

        ResponseEntity<String> response = restTemplate.postForEntity("/films", film, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonexistentFilm() {
        Film film = new Film();
        film.setId(999);
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(1);
        film.setGenreIds(Set.of(1));

        HttpEntity<Film> request = new HttpEntity<>(film);
        ResponseEntity<String> response = restTemplate.exchange("/films",
                org.springframework.http.HttpMethod.PUT, request, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
