package com.registration.userservice.integration;

import com.registration.userservice.dto.PersistenceResponse;
import com.registration.userservice.dto.RegistrationRequest;
import com.registration.userservice.entity.User;
import com.registration.userservice.repository.UserRepository;
import com.registration.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private RegistrationRequest validRequest;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        userRepository.deleteAll();

        validRequest = RegistrationRequest.builder()
                .dni(12345678)
                .nombre("Integration Test User")
                .correo("integration@test.com")
                .clave(5678)
                .telefono(999888777)
                .build();
    }

    @Test
    void persistUser_Success_UserSavedToDatabase() {
        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(validRequest.getDni(), response.getDni());
        
        // Verify user was saved
        User savedUser = userRepository.findByDni(validRequest.getDni()).orElse(null);
        assertNotNull(savedUser);
        assertEquals(validRequest.getNombre(), savedUser.getNombre());
        assertEquals(validRequest.getCorreo(), savedUser.getCorreo());
        assertEquals(validRequest.getClave(), savedUser.getClave());
        assertEquals(validRequest.getTelefono(), savedUser.getTelefono());
    }

    @Test
    void persistUser_WithFriends_CreatesRelationships() {
        // Arrange - Use unique DNIs for this test
        validRequest.setDni(22222222);
        validRequest.setCorreo("user2@test.com");
        
        // Create friend users first
        User friend1 = User.builder()
                .dni(87654321)
                .nombre("Friend One")
                .correo("friend1@test.com")
                .clave(1111)
                .telefono(111111111)
                .build();
        
        User friend2 = User.builder()
                .dni(11111111)
                .nombre("Friend Two")
                .correo("friend2@test.com")
                .clave(2222)
                .telefono(222222222)
                .build();
        
        userRepository.save(friend1);
        userRepository.save(friend2);
        
        // Set friends in request
        validRequest.setFriendsDni(Arrays.asList(87654321, 11111111));

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        
        // Verify user and friends relationship
        User savedUser = userService.findByDni(validRequest.getDni());
        assertNotNull(savedUser);
        assertEquals(2, savedUser.getFriends().size());
        assertTrue(savedUser.getFriends().stream()
                .anyMatch(f -> f.getDni().equals(87654321)));
        assertTrue(savedUser.getFriends().stream()
                .anyMatch(f -> f.getDni().equals(11111111)));
    }

    @Test
    void persistUser_WithNonExistentFriends_SucceedsButSkipsMissingFriends() {
        // Arrange - Use unique DNIs for this test
        validRequest.setDni(33333333);
        validRequest.setCorreo("user3@test.com");
        
        // Create only one friend
        User friend1 = User.builder()
                .dni(87654322)
                .nombre("Friend One")
                .correo("friend1b@test.com")
                .clave(1111)
                .telefono(111111112)
                .build();
        
        userRepository.save(friend1);
        
        // Request includes a non-existent friend
        validRequest.setFriendsDni(Arrays.asList(87654322, 99999999));

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        
        // Verify only existing friend was added
        User savedUser = userService.findByDni(validRequest.getDni());
        assertNotNull(savedUser);
        assertEquals(1, savedUser.getFriends().size());
        assertEquals(87654322, savedUser.getFriends().iterator().next().getDni());
    }

    @Test
    void persistUser_DuplicateDni_Fails() {
        // Arrange - Create existing user
        User existingUser = User.builder()
                .dni(44444444)
                .nombre("Existing User")
                .correo("existing@test.com")
                .clave(9999)
                .telefono(333333333)
                .build();
        userRepository.save(existingUser);

        // Update request to use same DNI
        validRequest.setDni(44444444);
        validRequest.setCorreo("different@test.com");

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getMessage().contains("already exists"));
        
        // Verify only one user exists
        assertEquals(1, userRepository.count());
    }

    @Test
    void persistUser_DuplicateEmail_Fails() {
        // Arrange - Create user with same email
        User existingUser = User.builder()
                .dni(55555555)
                .nombre("Different User")
                .correo("duplicate@test.com") 
                .clave(8888)
                .telefono(444444444)
                .build();
        userRepository.save(existingUser);

        // Update request to use same email but different DNI
        validRequest.setDni(66666666);
        validRequest.setCorreo("duplicate@test.com");

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getMessage().contains("email"));
        assertTrue(response.getMessage().contains("already exists"));
        
        // Verify only one user exists
        assertEquals(1, userRepository.count());
    }

    @Test
    void persistUser_EmptyFriendsList_Success() {
        // Arrange - Use unique DNI
        validRequest.setDni(77777777);
        validRequest.setCorreo("user7@test.com");
        validRequest.setFriendsDni(Collections.emptyList());

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        
        User savedUser = userService.findByDni(validRequest.getDni());
        assertNotNull(savedUser);
        assertTrue(savedUser.getFriends().isEmpty());
    }

    @Test
    void userExists_ReturnsTrueForExistingUser() {
        // Arrange
        User user = User.builder()
                .dni(88888888)
                .nombre("Test User")
                .correo("test8@example.com")
                .clave(1234)
                .telefono(555555555)
                .build();
        userRepository.save(user);

        // Act & Assert
        assertTrue(userService.userExists(88888888));
        assertFalse(userService.userExists(99999999));
    }

    @Test
    void findByDni_ReturnsUserWithFriends() {
        // Arrange
        User user = User.builder()
                .dni(10101010)
                .nombre("Main User")
                .correo("main@test.com")
                .clave(1234)
                .telefono(666666666)
                .build();
        
        User friend = User.builder()
                .dni(20202020)
                .nombre("Friend User")
                .correo("friend@test.com")
                .clave(5678)
                .telefono(777777777)
                .build();
        
        userRepository.save(user);
        userRepository.save(friend);
        
        user.addFriend(friend);
        userRepository.save(user);

        // Act
        User result = userService.findByDni(10101010);

        // Assert
        assertNotNull(result);
        assertEquals(10101010, result.getDni());
        assertEquals(1, result.getFriends().size());
        assertEquals(20202020, result.getFriends().iterator().next().getDni());
    }
} 