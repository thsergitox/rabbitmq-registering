package com.registration.userservice.service;

import com.registration.userservice.dto.PersistenceResponse;
import com.registration.userservice.dto.RegistrationRequest;
import com.registration.userservice.entity.User;
import com.registration.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private RegistrationRequest validRequest;
    private User savedUser;
    private User existingFriend1;
    private User existingFriend2;

    @BeforeEach
    void setUp() {
        validRequest = RegistrationRequest.builder()
                .dni(12345678)
                .nombre("John Doe")
                .correo("john.doe@example.com")
                .clave(1234)
                .telefono(987654321)
                .friendsDni(Arrays.asList(87654321, 11111111))
                .build();

        savedUser = User.builder()
                .id(1)
                .dni(12345678)
                .nombre("John Doe")
                .correo("john.doe@example.com")
                .clave(1234)
                .telefono(987654321)
                .build();

        existingFriend1 = User.builder()
                .id(2)
                .dni(87654321)
                .nombre("Friend One")
                .correo("friend1@example.com")
                .build();

        existingFriend2 = User.builder()
                .id(3)
                .dni(11111111)
                .nombre("Friend Two")
                .correo("friend2@example.com")
                .build();
    }

    @Test
    void persistUser_Success_WithoutFriends() {
        // Arrange
        validRequest.setFriendsDni(null);
        when(userRepository.existsByDni(validRequest.getDni())).thenReturn(false);
        when(userRepository.existsByCorreo(validRequest.getCorreo())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(validRequest.getDni(), response.getDni());
        assertTrue(response.getMessage().contains("successfully"));
        assertNotNull(response.getTimestamp());
        
        verify(userRepository).existsByDni(validRequest.getDni());
        verify(userRepository).existsByCorreo(validRequest.getCorreo());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void persistUser_Success_WithFriends() {
        // Arrange
        when(userRepository.existsByDni(validRequest.getDni())).thenReturn(false);
        when(userRepository.existsByCorreo(validRequest.getCorreo())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.findByDni(87654321)).thenReturn(Optional.of(existingFriend1));
        when(userRepository.findByDni(11111111)).thenReturn(Optional.of(existingFriend2));

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(validRequest.getDni(), response.getDni());
        
        verify(userRepository).findByDni(87654321);
        verify(userRepository).findByDni(11111111);
        verify(userRepository, times(2)).save(any(User.class)); // Once for user, once for friends
    }

    @Test
    void persistUser_Success_WithSomeFriendsNotFound() {
        // Arrange
        when(userRepository.existsByDni(validRequest.getDni())).thenReturn(false);
        when(userRepository.existsByCorreo(validRequest.getCorreo())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.findByDni(87654321)).thenReturn(Optional.of(existingFriend1));
        when(userRepository.findByDni(11111111)).thenReturn(Optional.empty()); // Friend not found

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(validRequest.getDni(), response.getDni());
        
        verify(userRepository).findByDni(87654321);
        verify(userRepository).findByDni(11111111);
    }

    @Test
    void persistUser_Failed_UserExistsByDni() {
        // Arrange
        when(userRepository.existsByDni(validRequest.getDni())).thenReturn(true);

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("FAILED", response.getStatus());
        assertEquals(validRequest.getDni(), response.getDni());
        assertTrue(response.getMessage().contains("already exists"));
        
        verify(userRepository).existsByDni(validRequest.getDni());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void persistUser_Failed_UserExistsByEmail() {
        // Arrange
        when(userRepository.existsByDni(validRequest.getDni())).thenReturn(false);
        when(userRepository.existsByCorreo(validRequest.getCorreo())).thenReturn(true);

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("FAILED", response.getStatus());
        assertEquals(validRequest.getDni(), response.getDni());
        assertTrue(response.getMessage().contains("email"));
        assertTrue(response.getMessage().contains("already exists"));
        
        verify(userRepository).existsByCorreo(validRequest.getCorreo());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void persistUser_Failed_ExceptionDuringSave() {
        // Arrange
        when(userRepository.existsByDni(validRequest.getDni())).thenReturn(false);
        when(userRepository.existsByCorreo(validRequest.getCorreo())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("FAILED", response.getStatus());
        assertEquals(validRequest.getDni(), response.getDni());
        assertTrue(response.getMessage().contains("Error persisting user"));
    }

    @Test
    void findByDni_UserExists() {
        // Arrange
        when(userRepository.findByDniWithFriends(12345678)).thenReturn(Optional.of(savedUser));

        // Act
        User result = userService.findByDni(12345678);

        // Assert
        assertNotNull(result);
        assertEquals(savedUser.getDni(), result.getDni());
        verify(userRepository).findByDniWithFriends(12345678);
    }

    @Test
    void findByDni_UserNotExists() {
        // Arrange
        when(userRepository.findByDniWithFriends(99999999)).thenReturn(Optional.empty());

        // Act
        User result = userService.findByDni(99999999);

        // Assert
        assertNull(result);
        verify(userRepository).findByDniWithFriends(99999999);
    }

    @Test
    void userExists_True() {
        // Arrange
        when(userRepository.existsByDni(12345678)).thenReturn(true);

        // Act
        boolean exists = userService.userExists(12345678);

        // Assert
        assertTrue(exists);
        verify(userRepository).existsByDni(12345678);
    }

    @Test
    void userExists_False() {
        // Arrange
        when(userRepository.existsByDni(99999999)).thenReturn(false);

        // Act
        boolean exists = userService.userExists(99999999);

        // Assert
        assertFalse(exists);
        verify(userRepository).existsByDni(99999999);
    }

    @Test
    void persistUser_EmptyFriendsList() {
        // Arrange
        validRequest.setFriendsDni(Collections.emptyList());
        when(userRepository.existsByDni(validRequest.getDni())).thenReturn(false);
        when(userRepository.existsByCorreo(validRequest.getCorreo())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        PersistenceResponse response = userService.persistUser(validRequest);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        verify(userRepository, times(1)).save(any(User.class)); // Only save user, no friends to add
    }
} 