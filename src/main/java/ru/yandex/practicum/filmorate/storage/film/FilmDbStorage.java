package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.util.*;

@Component("filmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void addFilm(Film film) {
        //if (film.getMpa() == null) {
        //    throw new IllegalArgumentException("У фильма должен быть указан рейтинг MPA.");
        //}
        //if (film.getGenres() == null || film.getGenres().isEmpty()) {
        //    throw new IllegalArgumentException("Фильм должен иметь хотя бы один жанр.");
        //}

        String sql = """
                INSERT INTO films (film_name, description, release_date, duration, mpa_rating_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa().getId()
        );

        Long filmId = jdbcTemplate.queryForObject("SELECT MAX(film_id) FROM films", Long.class);
        if (filmId == null) {
            throw new IllegalStateException("Не удалось получить ID добавленного фильма.");
        }
        film.setId(filmId);
        insertGenres(film);
    }

    @Override
    public void updateFilm(Film film) {
        //if (film.getGenres() == null || film.getGenres().isEmpty()) {
        //    throw new IllegalArgumentException("Фильм должен иметь хотя бы один жанр.");
        //}

        String sql = """
                UPDATE films
                SET film_name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ?
                WHERE film_id = ?
                """;

        int updated = jdbcTemplate.update(
                sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId()
        );

        if (updated == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        // Обновляем жанры
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        insertGenres(film);
    }

    @Override
    public void removeFilm(long id) {
        String checkSql = "SELECT COUNT(*) FROM films WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);
        if (count == null || count == 0) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", id);
        jdbcTemplate.update("DELETE FROM likes WHERE film_id = ?", id);
        jdbcTemplate.update("DELETE FROM films WHERE film_id = ?", id);
    }

    @Override
    public Film getFilm(long id) {
        String sql = "SELECT * FROM films WHERE film_id = ?";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, id);
        if (films.isEmpty()) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }

        Film film = films.get(0);
        film.setGenres(getGenresByFilmId(id));
        film.setLikes(getLikes(id));
        film.setMpa(getMpaByIdFromDb(film.getMpa().getId()));

        return film;
    }

    @Override
    public List<Film> getAllFilms() {
        List<Film> films = jdbcTemplate.query("SELECT * FROM films", filmRowMapper);
        for (Film film : films) {
            film.setGenres(getGenresByFilmId(film.getId()));
            film.setLikes(getLikes(film.getId()));
            film.setMpa(getMpaByIdFromDb(film.getMpa().getId()));
        }
        return films;
    }

    @Override
    public void addLike(long filmId, long userId) {
        getFilm(filmId);
        String sql = """
                INSERT INTO likes (film_id, user_id)
                SELECT ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM likes WHERE film_id = ? AND user_id = ?
                )
                """;
        jdbcTemplate.update(sql, filmId, userId, filmId, userId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        getFilm(filmId);
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Film> getMostPopularFilms(int count) {
        String sql = """
                SELECT f.film_id, f.film_name, f.description, f.release_date, f.duration, f.mpa_rating_id,
                       COUNT(l.user_id) AS likes_count
                FROM films f
                LEFT JOIN likes l ON f.film_id = l.film_id
                GROUP BY f.film_id, f.film_name, f.description, f.release_date, f.duration, f.mpa_rating_id
                ORDER BY likes_count DESC
                LIMIT ?
                """;
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, count);
        for (Film film : films) {
            film.setGenres(getGenresByFilmId(film.getId()));
            film.setLikes(getLikes(film.getId()));
            film.setMpa(getMpaByIdFromDb(film.getMpa().getId()));
        }
        return films;
    }

    // Жанры
    @Override
    public List<Genre> getAllGenres() {
        String sql = "SELECT genre_id, genre_name FROM genres ORDER BY genre_id";
        return jdbcTemplate.query(sql, genreRowMapper);
    }

    @Override
    public Genre getGenreById(int id) {
        String sql = "SELECT genre_id, genre_name FROM genres WHERE genre_id = ?";
        return jdbcTemplate.query(sql, genreRowMapper, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Жанр с id=" + id + " не найден"));
    }

    // MPA
    @Override
    public List<Mpa> getAllMpa() {
        String sql = "SELECT rating_id, rating_name FROM mpa_ratings ORDER BY rating_id";
        return jdbcTemplate.query(sql, mpaRowMapper);
    }

    @Override
    public Mpa getMpaById(int id) {
        String sql = "SELECT rating_id, rating_name FROM mpa_ratings WHERE rating_id = ?";
        return jdbcTemplate.query(sql, mpaRowMapper, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("MPA рейтинг с id=" + id + " не найден"));
    }

    // Вспомогательные методы

    private void insertGenres(Film film) {
        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        for (Genre genre : film.getGenres()) {
            jdbcTemplate.update(sql, film.getId(), genre.getId());
        }
    }

    private Set<Genre> getGenresByFilmId(long filmId) {
        String sql = """
                SELECT g.genre_id, g.genre_name
                FROM genres g
                JOIN film_genres fg ON g.genre_id = fg.genre_id
                WHERE fg.film_id = ?
                ORDER BY g.genre_id
                """;
        return new LinkedHashSet<>(jdbcTemplate.query(sql, genreRowMapper, filmId));
    }

    private Set<Long> getLikes(long filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, filmId));
    }

    private Mpa getMpaByIdFromDb(int id) {
        return getMpaById(id);
    }

    private final RowMapper<Film> filmRowMapper = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getLong("film_id"));
        film.setName(rs.getString("film_name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        // временно создаём MPA с id, имя потом подтянем из БД
        film.setMpa(new Mpa(rs.getInt("mpa_rating_id"), ""));
        return film;
    };

    private final RowMapper<Genre> genreRowMapper = (rs, rowNum) ->
            new Genre(rs.getInt("genre_id"), rs.getString("genre_name"));

    private final RowMapper<Mpa> mpaRowMapper = (rs, rowNum) ->
            new Mpa(rs.getInt("rating_id"), rs.getString("rating_name"));

}
