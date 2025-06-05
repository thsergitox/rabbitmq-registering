package com.registration.userservice.service;

import com.registration.userservice.dto.RegistrationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQConsumer {

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
            
            // TODO: In task 7, implement the actual persistence logic
            log.info("Registration request for DNI: {} queued for processing", request.getDni());
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid registration request for DNI: {} - {}", 
                     request.getDni(), e.getMessage());
            // TODO: In task 8, send error response via RabbitMQ
        } catch (Exception e) {
            log.error("Error processing registration request for DNI: {}", 
                     request.getDni(), e);
            // TODO: In task 8, send error response via RabbitMQ
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