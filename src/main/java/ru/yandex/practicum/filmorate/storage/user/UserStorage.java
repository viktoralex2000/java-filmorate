package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

public interface UserStorage {
    void addUser(User user);

    void removeUser(long id);

    void updateUser(User user);

    User getUser(long id);

    List<User> getFriends(long userId);

    List<User> getAllUsers();

    List<User> getCommonFriends(long userId, long otherId);

}
