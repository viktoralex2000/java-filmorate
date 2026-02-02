package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTest {
    private final UserDbStorage userStorage;
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        userStorage.getAllUsers()
                .forEach(u -> userStorage.removeUser(u.getId()));
        user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setLogin("user1");
        user1.setName("User One");
        user1.setBirthday(LocalDate.of(1990, 1, 1));
        user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2");
        user2.setName("User Two");
        user2.setBirthday(LocalDate.of(1991, 2, 2));
        user3 = new User();
        user3.setEmail("user3@example.com");
        user3.setLogin("user3");
        user3.setName("User Three");
        user3.setBirthday(LocalDate.of(1992, 3, 3));
    }

    @Test
    void testAddAndGetUser() {
        userStorage.addUser(user1);
        User fetched = userStorage.getUser(user1.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getEmail()).isEqualTo(user1.getEmail());
        assertThat(fetched.getLogin()).isEqualTo(user1.getLogin());
        assertThat(fetched.getName()).isEqualTo(user1.getName());
        assertThat(fetched.getBirthday()).isEqualTo(user1.getBirthday());
    }

    @Test
    void testUpdateUser() {
        userStorage.addUser(user1);
        user1.setName("Updated Name");
        user1.setEmail("updated@example.com");
        userStorage.updateUser(user1);
        User updated = userStorage.getUser(user1.getId());
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void testRemoveUser() {
        userStorage.addUser(user1);
        long id = user1.getId();
        userStorage.removeUser(id);
        assertThatThrownBy(() -> userStorage.getUser(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void testGetAllUsers() {
        userStorage.addUser(user1);
        userStorage.addUser(user2);
        List<User> users = userStorage.getAllUsers();
        assertThat(users).hasSize(2)
                .extracting(User::getLogin)
                .containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    void testGetFriendsAndCommonFriends() {
        userStorage.addUser(user1);
        userStorage.addUser(user2);
        userStorage.addUser(user3);
        userStorage.addFriend(user1.getId(), user2.getId());
        userStorage.addFriend(user2.getId(), user1.getId());
        userStorage.addFriend(user1.getId(), user3.getId());
        userStorage.addFriend(user3.getId(), user1.getId());
        userStorage.addFriend(user2.getId(), user3.getId());
        userStorage.addFriend(user3.getId(), user2.getId());
        Set<Long> friendsOfUser1 = userStorage.getUser(user1.getId()).getFriends();
        assertThat(friendsOfUser1)
                .containsExactlyInAnyOrder(user2.getId(), user3.getId());
        List<User> commonFriends = userStorage.getCommonFriends(user1.getId(), user2.getId());
        assertThat(commonFriends)
                .hasSize(1)
                .extracting(User::getId)
                .containsExactly(user3.getId());
    }
}
