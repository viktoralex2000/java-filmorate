package ru.yandex.practicum.filmorate.storage.film;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.io.IOException;
import java.util.*;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final HashMap<Long, Film> films = new HashMap<>();
    private long idCounter = 1;
    private Map<Integer, Genre> genres = new HashMap<>();
    private Map<Integer, Mpa> mpa = new HashMap<>();

    @PostConstruct
    public void init() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new ClassPathResource("reference-data.json").getInputStream());
        for (JsonNode node : root.get("genres")) {
            genres.put(node.get("id").asInt(), new Genre(node.get("id").asInt(), node.get("name").asText()));
        }
        for (JsonNode node : root.get("mpa")) {
            mpa.put(node.get("id").asInt(), new Mpa(node.get("id").asInt(), node.get("name").asText()));
        }
    }

    @Override
    public void addFilm(Film film) {
        film.setId(idCounter++);
        films.put(film.getId(), film);
    }

    @Override
    public void removeFilm(long id) {
        films.remove(id);
    }

    @Override
    public void updateFilm(Film film) {
        films.put(film.getId(), film);
    }

    @Override
    public Film getFilm(long id) {
        return films.get(id);
    }

    @Override
    public List<Film> getAllFilms() {
        return new ArrayList<>(films.values());
    }

    @Override
    public void addLike(long filmId, long userId) {
        Film film = getFilm(filmId);
        film.getLikes().add(userId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        Film film = getFilm(filmId);
        film.getLikes().remove(userId);
    }

    @Override
    public List<Film> getMostPopularFilms(int count) {
        return films.values().stream()
                .sorted((f1, f2) ->
                        Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .toList();
    }

    @Override
    public List<Genre> getAllGenres() {
        return new ArrayList<>(genres.values());
    }

    @Override
    public Genre getGenreById(int id) {
        return genres.get(id);
    }

    @Override
    public List<Mpa> getAllMpa() {
        return new ArrayList<>(mpa.values());
    }

    @Override
    public Mpa getMpaById(int id) {
        return mpa.get(id);
    }


}
