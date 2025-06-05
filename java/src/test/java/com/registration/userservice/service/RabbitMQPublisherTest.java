package com.registration.userservice.service;

import com.registration.userservice.dto.PersistenceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQPublisher rabbitMQPublisher;

    private final String exchangeName = "registro_bus";
    private final String persistedRoutingKey = "lp1.persisted";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rabbitMQPublisher, "exchangeName", exchangeName);
        ReflectionTestUtils.setField(rabbitMQPublisher, "persistedRoutingKey", persistedRoutingKey);
    }

    @Test
    void publishPersistenceResponse_Success() {
        // Given
        PersistenceResponse response = PersistenceResponse.builder()
                .dni(12345678)
                .status("SUCCESS")
                .message("User created successfully")
                .timestamp("2024-01-15T10:30:00")
                .build();

        // When
        rabbitMQPublisher.publishPersistenceResponse(response);

        // Then
        ArgumentCaptor<PersistenceResponse> responseCaptor = ArgumentCaptor.forClass(PersistenceResponse.class);
        verify(rabbitTemplate).convertAndSend(eq(exchangeName), eq(persistedRoutingKey), responseCaptor.capture());
        
        PersistenceResponse capturedResponse = responseCaptor.getValue();
        assertEquals(response.getDni(), capturedResponse.getDni());
        assertEquals(response.getStatus(), capturedResponse.getStatus());
        assertEquals(response.getMessage(), capturedResponse.getMessage());
        assertEquals(response.getTimestamp(), capturedResponse.getTimestamp());
    }

    @Test
    void publishPersistenceResponse_HandlesException() {
        // Given
        PersistenceResponse response = PersistenceResponse.builder()
                .dni(12345678)
                .status("FAILED")
                .message("Test error")
                .timestamp("2024-01-15T10:30:00")
                .build();

        doThrow(new RuntimeException("RabbitMQ connection error"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(PersistenceResponse.class));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> rabbitMQPublisher.publishPersistenceResponse(response));
        
        // Verify the attempt was made
        verify(rabbitTemplate).convertAndSend(eq(exchangeName), eq(persistedRoutingKey), eq(response));
    }

    @Test
    void publishErrorResponse_CreatesAndPublishesErrorResponse() {
        // Given
        Integer dni = 87654321;
        String errorMessage = "Duplicate user found";

        // When
        rabbitMQPublisher.publishErrorResponse(dni, errorMessage);

        // Then
        ArgumentCaptor<PersistenceResponse> responseCaptor = ArgumentCaptor.forClass(PersistenceResponse.class);
        verify(rabbitTemplate).convertAndSend(eq(exchangeName), eq(persistedRoutingKey), responseCaptor.capture());
        
        PersistenceResponse capturedResponse = responseCaptor.getValue();
        assertEquals(dni, capturedResponse.getDni());
        assertEquals("FAILED", capturedResponse.getStatus());
        assertEquals(errorMessage, capturedResponse.getMessage());
        assertNotNull(capturedResponse.getTimestamp());
    }

    @Test
    void publishSuccessResponse_CreatesAndPublishesSuccessResponse() {
        // Given
        Integer dni = 11223344;
        String successMessage = "User registered with 3 friends";

        // When
        rabbitMQPublisher.publishSuccessResponse(dni, successMessage);

        // Then
        ArgumentCaptor<PersistenceResponse> responseCaptor = ArgumentCaptor.forClass(PersistenceResponse.class);
        verify(rabbitTemplate).convertAndSend(eq(exchangeName), eq(persistedRoutingKey), responseCaptor.capture());
        
        PersistenceResponse capturedResponse = responseCaptor.getValue();
        assertEquals(dni, capturedResponse.getDni());
        assertEquals("SUCCESS", capturedResponse.getStatus());
        assertEquals(successMessage, capturedResponse.getMessage());
        assertNotNull(capturedResponse.getTimestamp());
    }

    @Test
    void publishSuccessResponse_WithNullMessage() {
        // Given
        Integer dni = 99887766;
        String successMessage = null;

        // When
        rabbitMQPublisher.publishSuccessResponse(dni, successMessage);

        // Then
        ArgumentCaptor<PersistenceResponse> responseCaptor = ArgumentCaptor.forClass(PersistenceResponse.class);
        verify(rabbitTemplate).convertAndSend(eq(exchangeName), eq(persistedRoutingKey), responseCaptor.capture());
        
        PersistenceResponse capturedResponse = responseCaptor.getValue();
        assertEquals(dni, capturedResponse.getDni());
        assertEquals("SUCCESS", capturedResponse.getStatus());
        assertNull(capturedResponse.getMessage());
        assertNotNull(capturedResponse.getTimestamp());
    }
} 