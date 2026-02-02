package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {
    void addFilm(Film film);

    void removeFilm(long id);

    void updateFilm(Film film);

    Film getFilm(long id);

    List<Film> getAllFilms();
}