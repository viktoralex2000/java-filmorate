package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FilmDbStorageTest {

    @Autowired
    private FilmDbStorage filmStorage;

    private Film film1;
    private Film film2;
    private Film film3;

    @BeforeEach
    void setUp() {
        filmStorage.getAllFilms().forEach(f -> filmStorage.removeFilm(f.getId()));

        film1 = createTestFilm("Film One", "Description One", LocalDate.of(2000, 1, 1), 120);
        film2 = createTestFilm("Film Two", "Description Two", LocalDate.of(2005, 5, 5), 90);
        film3 = createTestFilm("Film Three", "Description Three", LocalDate.of(2010, 10, 10), 150);
    }

    private Film createTestFilm(String name, String desc, LocalDate releaseDate, int duration) {
        Film film = new Film();
        film.setName(name);
        film.setDescription(desc);
        film.setReleaseDate(releaseDate);
        film.setDuration(duration);
        film.setMpa(1);
        film.setGenres(Set.of(1));
        return film;
    }

    @Test
    void testAddAndGetFilm() {
        filmStorage.addFilm(film1);
        Film retrieved = filmStorage.getFilm(film1.getId());

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo(film1.getName());
        assertThat(retrieved.getDescription()).isEqualTo(film1.getDescription());
        assertThat(retrieved.getReleaseDate()).isEqualTo(film1.getReleaseDate());
        assertThat(retrieved.getDuration()).isEqualTo(film1.getDuration());
        assertThat(retrieved.getMpa()).isEqualTo(1);
        assertThat(retrieved.getGenres()).containsExactly(1);
    }

    @Test
    void testUpdateFilm() {
        filmStorage.addFilm(film1);

        film1.setName("Updated Name");
        film1.setDescription("Updated Description");
        film1.setDuration(180);
        film1.setGenres(Set.of(2));

        filmStorage.updateFilm(film1);

        Film updated = filmStorage.getFilm(film1.getId());

        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getDescription()).isEqualTo("Updated Description");
        assertThat(updated.getDuration()).isEqualTo(180);
        assertThat(updated.getGenres()).containsExactly(2);
    }

    @Test
    void testRemoveFilm() {
        filmStorage.addFilm(film1);
        filmStorage.addFilm(film2);

        filmStorage.removeFilm(film1.getId());

        List<Film> allFilms = filmStorage.getAllFilms();
        assertThat(allFilms).hasSize(1).extracting(Film::getId).doesNotContain(film1.getId());
    }

    @Test
    void testGetAllFilms() {
        filmStorage.addFilm(film1);
        filmStorage.addFilm(film2);
        filmStorage.addFilm(film3);

        List<Film> allFilms = filmStorage.getAllFilms();
        assertThat(allFilms).hasSize(3)
                .extracting(Film::getId)
                .containsExactlyInAnyOrder(film1.getId(), film2.getId(), film3.getId());
    }

    @Autowired
    private UserDbStorage userStorage;

    @Test
    void testAddAndRemoveLike() {
        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setLogin("user1");
        user1.setName("User One");
        user1.setBirthday(LocalDate.of(1990, 1, 1));
        userStorage.addUser(user1);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2");
        user2.setName("User Two");
        user2.setBirthday(LocalDate.of(1991, 2, 2));
        userStorage.addUser(user2);

        long userId1 = user1.getId();
        long userId2 = user2.getId();

        filmStorage.addFilm(film1);
        filmStorage.addFilm(film2);

        filmStorage.addLike(film1.getId(), userId1);
        filmStorage.addLike(film1.getId(), userId2);
        filmStorage.addLike(film2.getId(), userId1);

        Film f1 = filmStorage.getFilm(film1.getId());
        Film f2 = filmStorage.getFilm(film2.getId());

        assertThat(f1.getLikes()).containsExactlyInAnyOrder(userId1, userId2);
        assertThat(f2.getLikes()).containsExactly(userId1);

        filmStorage.removeLike(film1.getId(), userId1);
        f1 = filmStorage.getFilm(film1.getId());
        assertThat(f1.getLikes()).containsExactly(userId2);
    }

}
