package com.registration.userservice.service;

import com.registration.userservice.dto.PersistenceResponse;
import com.registration.userservice.dto.RegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQConsumerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private RabbitMQConsumer rabbitMQConsumer;

    private RegistrationRequest validRequest;
    private PersistenceResponse successResponse;
    private PersistenceResponse failureResponse;

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

        successResponse = PersistenceResponse.builder()
                .dni(12345678)
                .status("SUCCESS")
                .message("User registered successfully")
                .timestamp("2025-01-05T10:00:00")
                .build();

        failureResponse = PersistenceResponse.builder()
                .dni(12345678)
                .status("FAILED")
                .message("User already exists")
                .timestamp("2025-01-05T10:00:00")
                .build();
    }

    @Test
    void consumeRegistrationRequest_ValidRequest_Success() {
        // Arrange
        when(userService.persistUser(any(RegistrationRequest.class))).thenReturn(successResponse);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
        
        verify(userService).persistUser(validRequest);
    }

    @Test
    void consumeRegistrationRequest_ValidRequest_Failure() {
        // Arrange
        when(userService.persistUser(any(RegistrationRequest.class))).thenReturn(failureResponse);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
        
        verify(userService).persistUser(validRequest);
    }

    @Test
    void consumeRegistrationRequest_ValidRequestWithoutFriends_ProcessesSuccessfully() {
        // Arrange
        validRequest.setFriendsDni(Collections.emptyList());
        when(userService.persistUser(any(RegistrationRequest.class))).thenReturn(successResponse);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
        
        verify(userService).persistUser(validRequest);
    }

    @Test
    void consumeRegistrationRequest_NullDni_LogsError() {
        // Arrange
        validRequest.setDni(null);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
        
        // Should not call userService due to validation failure
        verify(userService, never()).persistUser(any());
    }

    @Test
    void consumeRegistrationRequest_EmptyName_LogsError() {
        // Arrange
        validRequest.setNombre("");

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
        
        verify(userService, never()).persistUser(any());
    }

    @Test
    void consumeRegistrationRequest_NullEmail_LogsError() {
        // Arrange
        validRequest.setCorreo(null);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
        
        verify(userService, never()).persistUser(any());
    }

    @Test
    void consumeRegistrationRequest_NullPassword_LogsError() {
        // Arrange
        validRequest.setClave(null);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
        
        verify(userService, never()).persistUser(any());
    }

    @Test
    void consumeRegistrationRequest_NullPhone_LogsError() {
        // Arrange
        validRequest.setTelefono(null);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
        
        verify(userService, never()).persistUser(any());
    }

    @Test
    void consumeRegistrationRequest_UserServiceThrowsException_LogsError() {
        // Arrange
        when(userService.persistUser(any(RegistrationRequest.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
        
        verify(userService).persistUser(validRequest);
    }

    @Test
    void consumeRegistrationRequest_ValidRequest_LogsAppropriateMessages() {
        // Arrange
        when(userService.persistUser(any(RegistrationRequest.class))).thenReturn(successResponse);

        // Act
        rabbitMQConsumer.consumeRegistrationRequest(validRequest);

        // Assert
        verify(userService).persistUser(validRequest);
        // The actual log messages are verified through manual testing or log inspection
    }
} 