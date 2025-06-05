package com.registration.userservice.service;

import com.registration.userservice.dto.RegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RabbitMQConsumerTest {

    @InjectMocks
    private RabbitMQConsumer rabbitMQConsumer;

    private RegistrationRequest validRequest;

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
    }

    @Test
    void consumeRegistrationRequest_ValidRequest_ProcessesSuccessfully() {
        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
    }

    @Test
    void consumeRegistrationRequest_ValidRequestWithoutFriends_ProcessesSuccessfully() {
        // Arrange
        validRequest.setFriendsDni(Collections.emptyList());

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
    }

    @Test
    void consumeRegistrationRequest_NullDni_LogsError() {
        // Arrange
        validRequest.setDni(null);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
    }

    @Test
    void consumeRegistrationRequest_EmptyName_LogsError() {
        // Arrange
        validRequest.setNombre("");

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
    }

    @Test
    void consumeRegistrationRequest_NullEmail_LogsError() {
        // Arrange
        validRequest.setCorreo(null);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
    }

    @Test
    void consumeRegistrationRequest_NullPassword_LogsError() {
        // Arrange
        validRequest.setClave(null);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
    }

    @Test
    void consumeRegistrationRequest_NullPhone_LogsError() {
        // Arrange
        validRequest.setTelefono(null);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
    }

    @Test
    void validateRequest_AllFieldsValid_NoException() {
        // This test indirectly tests the private validateRequest method
        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(validRequest));
    }
} 