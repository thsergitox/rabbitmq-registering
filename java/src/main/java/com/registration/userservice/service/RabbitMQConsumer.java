package com.registration.userservice.service;

import com.registration.userservice.dto.PersistenceResponse;
import com.registration.userservice.dto.RegistrationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQConsumer {

    private final UserService userService;
    private final RabbitMQPublisher rabbitMQPublisher;

    @RabbitListener(queues = "${rabbitmq.queues.persist}")
    public void consumeRegistrationRequest(RegistrationRequest request) {
        log.info("Received registration request for DNI: {}", request.getDni());
        
        try {
            // Validate the incoming request
            validateRequest(request);
            
            // Log the request details
            log.debug("Processing registration for user: {} with email: {}", 
                     request.getNombre(), request.getCorreo());
            
            if (request.getFriendsDni() != null && !request.getFriendsDni().isEmpty()) {
                log.debug("User has {} friends to register", request.getFriendsDni().size());
            }
            
            // Persist the user using UserService
            PersistenceResponse response = userService.persistUser(request);
            
            // Publish the response to RabbitMQ
            rabbitMQPublisher.publishPersistenceResponse(response);
            
            if ("SUCCESS".equals(response.getStatus())) {
                log.info("Successfully persisted user with DNI: {}", request.getDni());
            } else {
                log.warn("Failed to persist user with DNI: {} - Reason: {}", 
                        request.getDni(), response.getMessage());
            }
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid registration request for DNI: {} - {}", 
                     request.getDni(), e.getMessage());
            // Send error response via RabbitMQ
            rabbitMQPublisher.publishErrorResponse(request.getDni(), 
                    "Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing registration request for DNI: {}", 
                     request.getDni(), e);
            // Send error response via RabbitMQ
            rabbitMQPublisher.publishErrorResponse(request.getDni(), 
                    "Unexpected error: " + e.getMessage());
        }
    }
    
    private void validateRequest(RegistrationRequest request) {
        if (request.getDni() == null) {
            throw new IllegalArgumentException("DNI is required");
        }
        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getCorreo() == null || request.getCorreo().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getClave() == null) {
            throw new IllegalArgumentException("Password is required");
        }
        if (request.getTelefono() == null) {
            throw new IllegalArgumentException("Phone is required");
        }
    }
} 