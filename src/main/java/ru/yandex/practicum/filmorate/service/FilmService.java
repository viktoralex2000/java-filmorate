package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.FilmRequestDto;
import ru.yandex.practicum.filmorate.dto.FilmResponseDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.util.List;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;
    private final FilmMapper filmMapper;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserService userService, FilmMapper filmMapper) {
        this.filmStorage = filmStorage;
        this.userService = userService;
        this.filmMapper = filmMapper;
    }

    public FilmResponseDto createFilm(FilmRequestDto request) {
        Film film = filmMapper.mapToFilm(request);
        long filmId = filmStorage.addFilm(film);
        return filmMapper.mapToResponseDto(getFilmById(filmId));
    }

    public FilmResponseDto updateFilm(FilmRequestDto request) {
        Film film = filmMapper.mapToFilm(request);
        long filmId = film.getId();
        Film existing = filmStorage.getFilm(filmId);
        if (existing == null) {
            throw new NotFoundException("Фильм с id=" + filmId + " не найден");
        }
        filmStorage.updateFilm(film);
        return filmMapper.mapToResponseDto(getFilmById(filmId));
    }

    public void deleteFilm(long id) {
        if (filmStorage.getFilm(id) == null) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        filmStorage.removeFilm(id);
    }

    public Film getFilmById(long id) {
        Film film = filmStorage.getFilm(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        return film;
    }

    public List<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public void addLike(long filmId, long userId) {
        getFilmById(filmId);
        userService.getUserById(userId);
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(long filmId, long userId) {
        getFilmById(filmId);
        userService.getUserById(userId);
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getMostPopularFilms(int count) {
        return filmStorage.getMostPopularFilms(count);
    }

    // Жанры и рейтинг MPA

    public List<Genre> getAllGenres() {
        return filmStorage.getAllGenres();
    }

    public Genre getGenreById(long id) {
        return filmStorage.getGenreById(id);
    }

    public List<Mpa> getAllMpa() {
        return filmStorage.getAllMpa();
    }

    public Mpa getMpaById(long id) {
        return filmStorage.getMpaById(id);
    }
}
