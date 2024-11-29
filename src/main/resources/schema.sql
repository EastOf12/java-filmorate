-- Таблица пользователей (User)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    login VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    birthday DATE NOT NULL
);

-- Таблица для друзей (friends)
CREATE TABLE IF NOT EXISTS user_friends (
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (friend_id) REFERENCES users(id)
);

-- Таблица фильмов (Film)
CREATE TABLE IF NOT EXISTS films (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    duration INT NOT NULL,
    release_date DATE NOT NULL
);

-- Таблица для лайков (likes)
CREATE TABLE IF NOT EXISTS film_likes (
    film_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (film_id, user_id),
    FOREIGN KEY (film_id) REFERENCES films(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Таблица для жанров (genres)
CREATE TABLE IF NOT EXISTS genres (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Таблица для связи фильмов и жанров (film_genres)
CREATE TABLE IF NOT EXISTS film_genres (
    film_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (film_id, genre_id),
    FOREIGN KEY (film_id) REFERENCES films(id),
    FOREIGN KEY (genre_id) REFERENCES genres(id)
);

-- Таблица для рейтингов (rating)
CREATE TABLE IF NOT EXISTS rating (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Таблица для связи фильмов и рейтингов (film_rating)
CREATE TABLE IF NOT EXISTS film_rating (
    film_id BIGINT NOT NULL,
    rating_id BIGINT NOT NULL,
    PRIMARY KEY (film_id, rating_id),
    FOREIGN KEY (film_id) REFERENCES films(id),
    FOREIGN KEY (rating_id) REFERENCES rating(id)
);