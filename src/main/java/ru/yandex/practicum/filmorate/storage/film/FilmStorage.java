package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

public interface FilmStorage {
    void addFilm(Film film);

    void removeFilm(long id);

    void updateFilm(Film film);

    Film getFilm(long id);

    List<Film> getAllFilms();

    // Методы для жанров
    List<Genre> getAllGenres();

    Genre getGenreById(int id);

    // Методы для рейтингов MPA
    List<Mpa> getAllMpa();

    Mpa getMpaById(int id);
}