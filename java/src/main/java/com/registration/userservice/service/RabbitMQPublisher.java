package com.registration.userservice.service;

import com.registration.userservice.dto.PersistenceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    
    @Value("${rabbitmq.routing-keys.persisted}")
    private String persistedRoutingKey;
    
    /**
     * Publishes a persistence response to the response queue
     * @param response The persistence response to publish
     */
    public void publishPersistenceResponse(PersistenceResponse response) {
        try {
            log.info("Publishing persistence response for DNI: {} with status: {}", 
                    response.getDni(), response.getStatus());
            
            // Send the message to the exchange with the persisted routing key
            rabbitTemplate.convertAndSend(exchangeName, persistedRoutingKey, response);
            
            log.debug("Successfully published response: {}", response);
        } catch (Exception e) {
            log.error("Error publishing persistence response for DNI: {}", 
                    response.getDni(), e);
            // We don't throw the exception to avoid disrupting the main flow
            // The error is logged for monitoring and debugging
        }
    }
    
    /**
     * Creates and publishes an error response
     * @param dni The DNI of the user
     * @param errorMessage The error message
     */
    public void publishErrorResponse(Integer dni, String errorMessage) {
        PersistenceResponse errorResponse = PersistenceResponse.builder()
                .dni(dni)
                .status("FAILED")
                .message(errorMessage)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
                
        publishPersistenceResponse(errorResponse);
    }
    
    /**
     * Creates and publishes a success response
     * @param dni The DNI of the user
     * @param message The success message
     */
    public void publishSuccessResponse(Integer dni, String message) {
        PersistenceResponse successResponse = PersistenceResponse.builder()
                .dni(dni)
                .status("SUCCESS")
                .message(message)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
                
        publishPersistenceResponse(successResponse);
    }
} 