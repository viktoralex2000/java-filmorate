package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    public Film createFilm(@Valid @RequestBody Film film) {
        log.info("Добавлен фильм: {}", film);
        return filmService.createFilm(film);
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film film) {
        log.info("Обновление фильма: {}", film);
        return filmService.updateFilm(film);
    }

    @DeleteMapping("/{id}")
    public void deleteFilm(@PathVariable long id) {
        log.info("Удаление фильма с id={}", id);
        filmService.deleteFilm(id);
    }

    @GetMapping
    public List<Film> getAllFilms() {
        log.info("Запрос списка всех фильмов");
        return filmService.getAllFilms();
    }

    @GetMapping("/{id}")
    public Film getFilmById(@PathVariable long id) {
        log.info("Запрос фильма с id={}", id);
        return filmService.getFilmById(id);
    }

    // Лайки

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable long id, @PathVariable long userId) {
        log.info("Пользователь {} ставит лайк фильму {}", userId, id);
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(@PathVariable long id, @PathVariable long userId) {
        log.info("Пользователь {} убирает лайк у фильма {}", userId, id);
        filmService.removeLike(id, userId);
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(@RequestParam(defaultValue = "10") int count) {
        log.info("Запрос популярных фильмов (count={})", count);
        return filmService.getMostPopularFilms(count);
    }

    // Получить все жанры
    @GetMapping("/genres")
    public List<Genre> getAllGenres() {
        log.info("Запрос списка всех жанров");
        return filmService.getAllGenres();
    }

    // Получить жанр по id
    @GetMapping("/genres/{id}")
    public Genre getGenreById(@PathVariable int id) {
        log.info("Запрос жанра с id={}", id);
        return filmService.getGenreById(id);
    }

    // Получить все рейтинги MPA
    @GetMapping("/mpa")
    public List<Mpa> getAllMpa() {
        log.info("Запрос списка всех рейтингов MPA");
        return filmService.getAllMpa();
    }

    // Получить рейтинг MPA по id
    @GetMapping("/mpa/{id}")
    public Mpa getMpaById(@PathVariable int id) {
        log.info("Запрос рейтинга MPA с id={}", id);
        return filmService.getMpaById(id);
    }

}
