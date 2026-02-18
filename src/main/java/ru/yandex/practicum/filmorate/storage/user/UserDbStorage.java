package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.UserRowMapper;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.stream.Collectors;

@Component("userDbStorage")
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper;

    @Override
    public void addUser(User user) {
        setDisplayNameIfEmpty(user);
        String sql = """
                INSERT INTO users (email, login, user_name, birthday)
                VALUES (?, ?, ?, ?)
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    new String[]{"user_id"}
            );
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        user.setId(id);
    }

    @Override
    public void updateUser(User user) {
        setDisplayNameIfEmpty(user);
        String sql = """
                UPDATE users
                SET email = ?, login = ?, user_name = ?, birthday = ?
                WHERE user_id = ?
                """;
        int updated = jdbcTemplate.update(
                sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId()
        );
        if (updated == 0) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }
    }

    @Override
    public void removeUser(long id) {
        String deleteFriendshipsSql = "DELETE FROM friendships WHERE user_id = ? OR friend_id = ?";
        jdbcTemplate.update(deleteFriendshipsSql, id, id);
        String deleteUserSql = "DELETE FROM users WHERE user_id = ?";
        int rows = jdbcTemplate.update(deleteUserSql, id);
        if (rows == 0) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
    }

    @Override
    public User getUser(long id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        List<User> users = jdbcTemplate.query(sql, (rs, rowNum) -> userRowMapper.mapRowToUser(rs), id);
        if (users.isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        User user = users.get(0);
        String friendsSql = """
                SELECT f.friend_id
                FROM friendships f
                JOIN friendship_status fs ON f.status_id = fs.status_id
                JOIN friendships f_rev ON f.friend_id = f_rev.user_id AND f.user_id = f_rev.friend_id
                JOIN friendship_status fs_rev ON f_rev.status_id = fs_rev.status_id
                WHERE f.user_id = ? AND fs.status_name = 'confirmed' AND fs_rev.status_name = 'confirmed'
                """;
        List<Long> friendIds = jdbcTemplate.queryForList(friendsSql, Long.class, id);
        user.getFriends().clear();
        user.getFriends().addAll(friendIds);
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = jdbcTemplate.query(
                "SELECT * FROM users",
                (rs, rowNum) -> userRowMapper.mapRowToUser(rs)
        );
        if (users.isEmpty()) {
            return users;
        }
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();
        Map<Long, Set<Long>> friendsMap = getFriendsByUserIds(userIds);
        for (User user : users) {
            user.getFriends().clear();
            user.getFriends().addAll(
                    friendsMap.getOrDefault(user.getId(), new HashSet<>())
            );
        }
        return users;
    }

    @Override
    public void addFriend(long userId, long friendId) {
        getUser(userId);
        getUser(friendId);
        Integer pendingId = jdbcTemplate.queryForObject(
                "SELECT status_id FROM friendship_status WHERE status_name = 'pending'",
                Integer.class
        );
        Integer confirmedId = jdbcTemplate.queryForObject(
                "SELECT status_id FROM friendship_status WHERE status_name = 'confirmed'",
                Integer.class
        );
        String sqlCheck = "SELECT status_id FROM friendships WHERE user_id = ? AND friend_id = ?";
        List<Integer> existingStatus = jdbcTemplate.queryForList(sqlCheck, Integer.class, friendId, userId);
        if (!existingStatus.isEmpty()) {
            Integer statusId = existingStatus.get(0);
            if (statusId.equals(pendingId)) {
                String updateSql = "UPDATE friendships SET status_id = ? WHERE user_id = ? AND friend_id = ?";
                jdbcTemplate.update(updateSql, confirmedId, friendId, userId);
                jdbcTemplate.update(
                        "INSERT INTO friendships (user_id, friend_id, status_id) VALUES (?, ?, ?)",
                        userId, friendId, confirmedId
                );
                return;
            }
        }
        jdbcTemplate.update(
                "INSERT INTO friendships (user_id, friend_id, status_id) VALUES (?, ?, ?)",
                userId, friendId, pendingId
        );
    }

    @Override
    public void removeFriend(long userId, long friendId) {
        getUser(userId);
        getUser(friendId);
        Integer pendingId = jdbcTemplate.queryForObject(
                "SELECT status_id FROM friendship_status WHERE status_name = 'pending'",
                Integer.class
        );
        Integer confirmedId = jdbcTemplate.queryForObject(
                "SELECT status_id FROM friendship_status WHERE status_name = 'confirmed'",
                Integer.class
        );
        String sqlCheck = "SELECT status_id FROM friendships WHERE user_id = ? AND friend_id = ?";
        List<Integer> reverseStatus = jdbcTemplate.queryForList(sqlCheck, Integer.class, friendId, userId);
        jdbcTemplate.update("DELETE FROM friendships WHERE user_id = ? AND friend_id = ?", userId, friendId);
        if (!reverseStatus.isEmpty() && reverseStatus.get(0).equals(confirmedId)) {
            jdbcTemplate.update("DELETE FROM friendships WHERE user_id = ? AND friend_id = ?", friendId, userId);
        }
    }

    @Override
    public List<User> getFriends(long userId) {
        getUser(userId);
        String sql = """
                SELECT u.*
                FROM users u
                JOIN friendships f ON u.user_id = f.friend_id
                WHERE f.user_id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> userRowMapper.mapRowToUser(rs), userId);
    }

    @Override
    public List<User> getCommonFriends(long userId, long otherId) {
        String sql = """
                SELECT u.*
                FROM users u
                WHERE u.user_id IN (
                    SELECT f1.friend_id
                    FROM friendships f1
                    WHERE f1.user_id = ?
                )
                AND u.user_id IN (
                    SELECT f2.friend_id
                    FROM friendships f2
                    WHERE f2.user_id = ?
                )
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> userRowMapper.mapRowToUser(rs), userId, otherId);
    }

    public void clear() {
        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM users");
    }

    //Вспомогательные методы

    private void setDisplayNameIfEmpty(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    private Map<Long, Set<Long>> getFriendsByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT user_id, friend_id
                FROM friendships
                WHERE user_id IN (%s)
                """.formatted(
                userIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","))
        );
        Map<Long, Set<Long>> result = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> row : rows) {
            long userId = ((Number) row.get("user_id")).longValue();
            long friendId = ((Number) row.get("friend_id")).longValue();
            result.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        }
        return result;
    }

}
