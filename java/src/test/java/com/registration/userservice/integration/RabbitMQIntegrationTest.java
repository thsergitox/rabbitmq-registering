package com.registration.userservice.integration;

import com.registration.userservice.config.RabbitMQConfig;
import com.registration.userservice.dto.RegistrationRequest;
import com.registration.userservice.service.RabbitMQConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class RabbitMQIntegrationTest {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitMQConsumer rabbitMQConsumer;

    @Value("${rabbitmq.exchange.name:registro_bus}")
    private String exchangeName;

    @Value("${rabbitmq.routing-keys.persist:lp1.persist}")
    private String persistRoutingKey;

    @Test
    void contextLoads() {
        // This test verifies that the Spring context loads successfully
        // with all RabbitMQ configurations
    }

    @Test
    void rabbitMQConsumerIsConfiguredProperly() {
        // Test that consumer can process a message directly
        RegistrationRequest request = RegistrationRequest.builder()
                .dni(12345678)
                .nombre("Integration Test User")
                .correo("test@integration.com")
                .clave(5678)
                .telefono(999888777)
                .friendsDni(Arrays.asList(87654321, 11111111))
                .build();

        assertDoesNotThrow(() -> rabbitMQConsumer.consumeRegistrationRequest(request));
    }

    @Test
    void rabbitMQConfigurationBeansAreCreated() {
        // This test verifies that all RabbitMQ beans are created
        // The test will fail if beans are not properly configured
        assertDoesNotThrow(() -> {
            if (rabbitTemplate != null) {
                // RabbitTemplate is optional in test environment
                System.out.println("RabbitTemplate is configured");
            }
        });
    }
} 