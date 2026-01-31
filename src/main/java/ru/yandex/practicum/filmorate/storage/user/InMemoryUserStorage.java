package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final HashMap<Long, User> users = new HashMap<>();
    private long idCounter = 1;

    @Override
    public void addUser(User user) {
        user.setId(idCounter++);
        setDisplayNameIfEmpty(user);
        users.put(user.getId(), user);
    }

    @Override
    public void removeUser(long id) {
        users.remove(id);
    }

    @Override
    public void updateUser(User user) {
        users.put(user.getId(), user);
    }

    @Override
    public User getUser(long id) {
        return users.get(id);
    }

    @Override
    public List<User> getFriends(long userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        return user.getFriends().stream()
                .map(users::get)
                .filter(friend -> friend.getFriends().contains(userId))
                .toList();
    }

    @Override
    public List<User> getCommonFriends(long userId, long otherId) {
        User user = users.get(userId);
        User other = users.get(otherId);

        if (user == null || other == null) {
            throw new NotFoundException("Один из пользователей не найден");
        }

        Set<Long> userConfirmed = user.getFriends().stream()
                .filter(id -> {
                    User friend = users.get(id);
                    return friend != null && friend.getFriends().contains(userId);
                })
                .collect(Collectors.toSet());

        Set<Long> otherConfirmed = other.getFriends().stream()
                .filter(id -> {
                    User friend = users.get(id);
                    return friend != null && friend.getFriends().contains(otherId);
                })
                .collect(Collectors.toSet());

        userConfirmed.retainAll(otherConfirmed);

        return userConfirmed.stream()
                .map(users::get)
                .toList();
    }


    @Override
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    private void setDisplayNameIfEmpty(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
