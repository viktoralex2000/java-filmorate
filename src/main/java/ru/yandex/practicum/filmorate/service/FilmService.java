package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.FilmRequestDto;
import ru.yandex.practicum.filmorate.dto.FilmResponseDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage, UserService userService) {
        this.filmStorage = filmStorage;
        this.userService = userService;
    }

    /*public Film createFilm(Film film) {
        filmStorage.addFilm(film);
        return film;
    }*/

    public FilmResponseDto createFilm(FilmRequestDto request) {
        Film film = mapToFilm(request);
        validateFilm(film);
        filmStorage.addFilm(film);
        return mapToResponseDto(film);
    }

    /*public Film updateFilm(Film film) {
        if (filmStorage.getFilm(film.getId()) == null) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        filmStorage.updateFilm(film);
        return film;
    }*/

    public FilmResponseDto updateFilm(FilmRequestDto request) {
        Film film = mapToFilm(request);
        Film existing = filmStorage.getFilm(film.getId());
        if (existing == null) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        validateFilm(film);
        filmStorage.updateFilm(film);
        return mapToResponseDto(film);
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

    public Genre getGenreById(int id) {
        return filmStorage.getGenreById(id);
    }

    public List<Mpa> getAllMpa() {
        return filmStorage.getAllMpa();
    }

    public Mpa getMpaById(int id) {
        return filmStorage.getMpaById(id);
    }

    private Film mapToFilm(FilmRequestDto dto) {
        Film film = new Film();
        film.setName(dto.getName());
        film.setDescription(dto.getDescription());
        film.setReleaseDate(dto.getReleaseDate());
        film.setDuration(dto.getDuration());
        if (dto.getMpaId() != null) {
            Mpa mpa = getMpaById(dto.getMpaId());
            film.setMpa(mpa);
        }
        Set<Genre> genres = new HashSet<>();
        if (dto.getGenreIds() != null) {
            for (Integer genreId : dto.getGenreIds()) {
                genres.add(getGenreById(genreId));
            }
        }
        film.setGenres(genres);
        return film;
    }

    private FilmResponseDto mapToResponseDto(Film film) {
        FilmResponseDto dto = new FilmResponseDto();
        dto.setId(film.getId());
        dto.setName(film.getName());
        dto.setDescription(film.getDescription());
        dto.setReleaseDate(film.getReleaseDate());
        dto.setDuration(film.getDuration());
        dto.setMpa(film.getMpa());
        dto.setGenres(film.getGenres());
        return dto;
    }

    private void validateFilm(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == 0) {
            throw new IllegalArgumentException("У фильма должен быть указан рейтинг MPA.");
        }
        getMpaById(film.getMpa().getId());
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            throw new IllegalArgumentException("Фильм должен иметь хотя бы один жанр.");
        }
        for (Genre genre : film.getGenres()) {
            getGenreById(genre.getId());
        }
        if (film.getDuration() <= 0) {
            throw new IllegalArgumentException("Продолжительность фильма должна быть положительным числом.");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate()
                .isBefore(LocalDate.of(1895, 12, 28))) {
            throw new IllegalArgumentException("Дата релиза не может быть раньше 28.12.1895.");
        }
    }
}
