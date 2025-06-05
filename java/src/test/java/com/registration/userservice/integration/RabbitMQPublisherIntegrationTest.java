package com.registration.userservice.integration;

import com.registration.userservice.config.RabbitMQConfig;
import com.registration.userservice.dto.PersistenceResponse;
import com.registration.userservice.service.RabbitMQPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {RabbitMQPublisher.class, RabbitMQConfig.class})
@ActiveProfiles("test")
class RabbitMQPublisherIntegrationTest {

    @Autowired
    private RabbitMQPublisher rabbitMQPublisher;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        // Verify that the Spring context loads successfully with RabbitMQPublisher
    }

    @Test
    void publishPersistenceResponse_IntegrationTest() {
        // Given
        PersistenceResponse response = PersistenceResponse.builder()
                .dni(12345678)
                .status("SUCCESS")
                .message("Integration test message")
                .timestamp("2024-01-15T12:00:00")
                .build();

        // When
        rabbitMQPublisher.publishPersistenceResponse(response);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq("registro_bus"),
                eq("lp1.persisted"),
                eq(response)
        );
    }

    @Test
    void publishErrorResponse_IntegrationTest() {
        // Given
        Integer dni = 87654321;
        String errorMessage = "Integration test error";

        // When
        rabbitMQPublisher.publishErrorResponse(dni, errorMessage);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq("registro_bus"),
                eq("lp1.persisted"),
                argThat((PersistenceResponse response) -> 
                    response.getDni().equals(dni) &&
                    response.getStatus().equals("FAILED") &&
                    response.getMessage().equals(errorMessage)
                )
        );
    }

    @Test
    void publishSuccessResponse_IntegrationTest() {
        // Given
        Integer dni = 11223344;
        String successMessage = "Integration test success";

        // When
        rabbitMQPublisher.publishSuccessResponse(dni, successMessage);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq("registro_bus"),
                eq("lp1.persisted"),
                argThat((PersistenceResponse response) -> 
                    response.getDni().equals(dni) &&
                    response.getStatus().equals("SUCCESS") &&
                    response.getMessage().equals(successMessage)
                )
        );
    }
} 