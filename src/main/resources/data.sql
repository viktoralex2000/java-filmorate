MERGE INTO friendship_status (status_id, status_name) KEY(status_id)
    VALUES
    (1, 'pending'),
    (2, 'confirmed');

MERGE INTO mpa_ratings (rating_id, rating_name) KEY(rating_id)
    VALUES
    (1, '0+'),
    (2, '6+'),
    (3, '12+'),
    (4, '16+'),
    (5, '18+');

MERGE INTO genres (genre_id, genre_name) KEY(genre_id)
    VALUES
    (1, 'Комедия'),
    (2, 'Драма'),
    (3, 'Мультфильм'),
    (4, 'Триллер'),
    (5, 'Документальный'),
    (6, 'Боевик');
