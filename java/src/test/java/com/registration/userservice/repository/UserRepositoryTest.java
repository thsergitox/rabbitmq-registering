package com.registration.userservice.repository;

import com.registration.userservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        // Clear database
        entityManager.clear();

        // Create test users
        user1 = User.builder()
                .nombre("Juan Pérez")
                .correo("juan.perez@example.com")
                .clave(1234)
                .dni(12345678)
                .telefono(987654321)
                .build();

        user2 = User.builder()
                .nombre("María García")
                .correo("maria.garcia@example.com")
                .clave(5678)
                .dni(87654321)
                .telefono(912345678)
                .build();

        user3 = User.builder()
                .nombre("Carlos López")
                .correo("carlos.lopez@example.com")
                .clave(9012)
                .dni(11111111)
                .telefono(923456789)
                .build();

        // Save users
        user1 = entityManager.persistAndFlush(user1);
        user2 = entityManager.persistAndFlush(user2);
        user3 = entityManager.persistAndFlush(user3);
    }

    @Test
    @DisplayName("Should find user by correo")
    void shouldFindUserByCorreo() {
        // When
        Optional<User> found = userRepository.findByCorreo("juan.perez@example.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals(user1.getId(), found.get().getId());
        assertEquals("Juan Pérez", found.get().getNombre());
    }

    @Test
    @DisplayName("Should return empty when correo not found")
    void shouldReturnEmptyWhenCorreoNotFound() {
        // When
        Optional<User> found = userRepository.findByCorreo("notfound@example.com");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should find user by DNI")
    void shouldFindUserByDni() {
        // When
        Optional<User> found = userRepository.findByDni(12345678);

        // Then
        assertTrue(found.isPresent());
        assertEquals(user1.getId(), found.get().getId());
        assertEquals("juan.perez@example.com", found.get().getCorreo());
    }

    @Test
    @DisplayName("Should check if user exists by correo")
    void shouldCheckExistsByCorreo() {
        // When & Then
        assertTrue(userRepository.existsByCorreo("maria.garcia@example.com"));
        assertFalse(userRepository.existsByCorreo("notfound@example.com"));
    }

    @Test
    @DisplayName("Should check if user exists by DNI")
    void shouldCheckExistsByDni() {
        // When & Then
        assertTrue(userRepository.existsByDni(87654321));
        assertFalse(userRepository.existsByDni(99999999));
    }

    @Test
    @DisplayName("Should find user with friends by ID")
    void shouldFindUserWithFriendsById() {
        // Given
        user1.addFriend(user2);
        user1.addFriend(user3);
        userRepository.save(user1);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<User> found = userRepository.findByIdWithFriends(user1.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getFriends().size());
    }

    @Test
    @DisplayName("Should find user with friends by DNI")
    void shouldFindUserWithFriendsByDni() {
        // Given
        user1.addFriend(user2);
        userRepository.save(user1);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<User> found = userRepository.findByDniWithFriends(12345678);

        // Then
        assertTrue(found.isPresent());
        assertEquals(1, found.get().getFriends().size());
    }

    @Test
    @DisplayName("Should find users by DNI list")
    void shouldFindUsersByDniList() {
        // Given
        List<Integer> dnis = Arrays.asList(12345678, 87654321, 99999999);

        // When
        List<User> found = userRepository.findByDniIn(dnis);

        // Then
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(u -> u.getDni().equals(12345678)));
        assertTrue(found.stream().anyMatch(u -> u.getDni().equals(87654321)));
    }

    @Test
    @DisplayName("Should find users with friends by DNI list")
    void shouldFindUsersWithFriendsByDniList() {
        // Given
        user1.addFriend(user2);
        user2.addFriend(user3);
        userRepository.save(user1);
        userRepository.save(user2);
        entityManager.flush();
        entityManager.clear();

        List<Integer> dnis = Arrays.asList(12345678, 87654321);

        // When
        List<User> found = userRepository.findByDniInWithFriends(dnis);

        // Then
        assertEquals(2, found.size());
        
        User foundUser1 = found.stream()
                .filter(u -> u.getDni().equals(12345678))
                .findFirst().orElse(null);
        assertNotNull(foundUser1);
        assertEquals(1, foundUser1.getFriends().size());

        User foundUser2 = found.stream()
                .filter(u -> u.getDni().equals(87654321))
                .findFirst().orElse(null);
        assertNotNull(foundUser2);
        assertEquals(1, foundUser2.getFriends().size());
    }

    @Test
    @DisplayName("Should save and retrieve user with all fields")
    void shouldSaveAndRetrieveUserWithAllFields() {
        // Given
        User newUser = User.builder()
                .nombre("Ana Martínez")
                .correo("ana.martinez@example.com")
                .clave(3456)
                .dni(22222222)
                .telefono(934567890)
                .build();

        // When
        User saved = userRepository.save(newUser);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findById(saved.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals("Ana Martínez", found.get().getNombre());
        assertEquals("ana.martinez@example.com", found.get().getCorreo());
        assertEquals(3456, found.get().getClave());
        assertEquals(22222222, found.get().getDni());
        assertEquals(934567890, found.get().getTelefono());
    }
} 