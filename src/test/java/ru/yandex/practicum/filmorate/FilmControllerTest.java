package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class FilmControllerTest {

    private FilmController filmController;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
    }

    @Test
    void shouldThrowExceptionWhenNameIsEmpty() {
        Film film = new Film();
        film.setName("");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> filmController.addFilm(film),
                "Ошибка при пустом названии фильма");
    }

    @Test
    void shouldThrowExceptionWhenDescriptionTooLong() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("A".repeat(201)); // > 200 символов
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> filmController.addFilm(film),
                "Ошибка при слишком длинном описании фильма");
    }

    @Test
    void shouldThrowExceptionWhenReleaseDateBeforeCinemaBirthday() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(1800, 1, 1)); // до 1895-12-28
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> filmController.addFilm(film),
                "Ошибка при дате релиза до рождения кино");
    }

    @Test
    void shouldAcceptReleaseDateOnCinemaBirthday() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(1895, 12, 28)); // граничное значение
        film.setDuration(100);

        Film createdFilm = filmController.addFilm(film);
        assertNotNull(createdFilm.getId(), "Фильм должен успешно создаться на дату 28.12.1895");
    }

    @Test
    void shouldThrowExceptionWhenDurationIsZeroOrNegative() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0);

        assertThrows(ValidationException.class, () -> filmController.addFilm(film),
                "Ошибка при нулевой или отрицательной длительности фильма");
    }

    @Test
    void shouldCreateFilmSuccessfully() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Film createdFilm = filmController.addFilm(film);

        assertNotNull(createdFilm.getId(), "ID должен быть присвоен при создании фильма");
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

        Film createdFilm = filmController.addFilm(film);

        createdFilm.setDescription("Другое описание");
        Film updatedFilm = filmController.updateFilm(createdFilm);

        assertEquals("Другое описание", updatedFilm.getDescription(),
                "Описание фильма должно обновляться");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonexistentFilm() {
        Film film = new Film();
        film.setId(999); // несуществующий ID
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        assertThrows(ValidationException.class, () -> filmController.updateFilm(film),
                "Ошибка при обновлении несуществующего фильма");
    }
}
