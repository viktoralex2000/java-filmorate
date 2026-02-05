package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.mapper.MpaRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Component("filmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;
    private final GenreRowMapper genreRowMapper;
    private final MpaRowMapper mpaRowMapper;

    @Override
    public long addFilm(Film film) {
        if (film.getMpa() != null) {
            getMpaById(film.getMpa().getId());
        }
        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                getGenreById(genre.getId());
            }
        }
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
        film.setMpa(getMpaById(film.getMpa().getId()));
        film.setGenres(getGenresByFilmId(film.getId()));
        return filmId;
    }

    @Override
    public void updateFilm(Film film) {
        if (film.getMpa() != null) {
            getMpaById(film.getMpa().getId());
        }
        for (Genre genre : film.getGenres()) {
            getGenreById(genre.getId());
        }
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
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        insertGenres(film);
        film.setMpa(getMpaById(film.getMpa().getId()));
        film.setGenres(getGenresByFilmId(film.getId()));
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
        film.setMpa(getMpaById(film.getMpa().getId()));
        return film;
    }

    @Override
    public List<Film> getAllFilms() {
        String sql = """
                SELECT f.film_id, f.film_name, f.description, f.release_date, f.duration,
                       m.rating_id AS mpa_id, m.rating_name AS mpa_name
                FROM films f
                JOIN mpa_ratings m ON f.mpa_rating_id = m.rating_id
                """;
        List<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Film film = new Film();
            film.setId(rs.getLong("film_id"));
            film.setName(rs.getString("film_name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));
            film.setMpa(new Mpa(rs.getInt("mpa_id"), rs.getString("mpa_name")));
            return film;
        });
        if (films.isEmpty()) {
            return films;
        }
        List<Long> filmIds = films.stream().map(Film::getId).toList();
        String genreSql = """
                SELECT fg.film_id, g.genre_id, g.genre_name
                FROM film_genres fg
                JOIN genres g ON fg.genre_id = g.genre_id
                WHERE fg.film_id IN (%s)
                """.formatted(filmIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        List<Map<String, Object>> genreRows = jdbcTemplate.queryForList(genreSql);
        Map<Long, Set<Genre>> filmGenres = new HashMap<>();
        for (Map<String, Object> row : genreRows) {
            long filmId = ((Number) row.get("film_id")).longValue();
            Genre genre = new Genre(((Number) row.get("genre_id")).intValue(), (String) row.get("genre_name"));
            filmGenres.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        }
        String likesSql = "SELECT film_id, user_id FROM likes WHERE film_id IN (" +
                filmIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
        List<Map<String, Object>> likeRows = jdbcTemplate.queryForList(likesSql);
        Map<Long, Set<Long>> filmLikes = new HashMap<>();
        for (Map<String, Object> row : likeRows) {
            long filmId = ((Number) row.get("film_id")).longValue();
            long userId = ((Number) row.get("user_id")).longValue();
            filmLikes.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        }
        for (Film film : films) {
            film.setGenres(filmGenres.getOrDefault(film.getId(), new LinkedHashSet<>()));
            film.setLikes(filmLikes.getOrDefault(film.getId(), new HashSet<>()));
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
                SELECT f.film_id, f.film_name, f.description, f.release_date, f.duration,
                       m.rating_id AS mpa_id, m.rating_name AS mpa_name,
                       COUNT(l.user_id) AS likes_count
                FROM films f
                JOIN mpa_ratings m ON f.mpa_rating_id = m.rating_id
                LEFT JOIN likes l ON f.film_id = l.film_id
                GROUP BY f.film_id, f.film_name, f.description, f.release_date, f.duration,
                         m.rating_id, m.rating_name
                ORDER BY likes_count DESC
                LIMIT ?
                """;
        List<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Film film = new Film();
            film.setId(rs.getLong("film_id"));
            film.setName(rs.getString("film_name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));
            film.setMpa(new Mpa(rs.getInt("mpa_id"), rs.getString("mpa_name")));
            return film;
        }, count);
        if (films.isEmpty()) {
            return films;
        }
        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .toList();
        String genresSql = """
                SELECT fg.film_id, g.genre_id, g.genre_name
                FROM film_genres fg
                JOIN genres g ON fg.genre_id = g.genre_id
                WHERE fg.film_id IN (%s)
                """.formatted(
                filmIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))
        );
        Map<Long, Set<Genre>> genresMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(genresSql)) {
            long filmId = ((Number) row.get("film_id")).longValue();
            Genre genre = new Genre(
                    ((Number) row.get("genre_id")).intValue(),
                    (String) row.get("genre_name")
            );
            genresMap.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        }
        String likesSql = """
                SELECT film_id, user_id
                FROM likes
                WHERE film_id IN (%s)
                """.formatted(
                filmIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))
        );
        Map<Long, Set<Long>> likesMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(likesSql)) {
            long filmId = ((Number) row.get("film_id")).longValue();
            long userId = ((Number) row.get("user_id")).longValue();
            likesMap.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        }
        for (Film film : films) {
            film.setGenres(genresMap.getOrDefault(film.getId(), new LinkedHashSet<>()));
            film.setLikes(likesMap.getOrDefault(film.getId(), new HashSet<>()));
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
    public Genre getGenreById(long id) {
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
    public Mpa getMpaById(long id) {
        String sql = "SELECT rating_id, rating_name FROM mpa_ratings WHERE rating_id = ?";
        return jdbcTemplate.query(sql, mpaRowMapper, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("MPA рейтинг с id=" + id + " не найден"));
    }

    public void clear() {
        jdbcTemplate.update("DELETE FROM likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM films");
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

}
