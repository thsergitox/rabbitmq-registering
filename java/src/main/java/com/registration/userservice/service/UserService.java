package com.registration.userservice.service;

import com.registration.userservice.dto.PersistenceResponse;
import com.registration.userservice.dto.RegistrationRequest;
import com.registration.userservice.entity.User;
import com.registration.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Transactional
    public PersistenceResponse persistUser(RegistrationRequest request) {
        try {
            // Check if user already exists by DNI
            if (userRepository.existsByDni(request.getDni())) {
                log.warn("User with DNI {} already exists", request.getDni());
                return buildResponse(request.getDni(), "FAILED", 
                    "User with DNI " + request.getDni() + " already exists");
            }

            // Check if user already exists by email
            if (userRepository.existsByCorreo(request.getCorreo())) {
                log.warn("User with email {} already exists", request.getCorreo());
                return buildResponse(request.getDni(), "FAILED", 
                    "User with email " + request.getCorreo() + " already exists");
            }

            // Create new user
            User newUser = User.builder()
                    .nombre(request.getNombre())
                    .correo(request.getCorreo())
                    .clave(request.getClave())
                    .dni(request.getDni())
                    .telefono(request.getTelefono())
                    .build();

            // Save the user first
            newUser = userRepository.save(newUser);
            log.info("User created successfully with ID: {} and DNI: {}", newUser.getId(), newUser.getDni());

            // Handle friend relationships if any
            if (request.getFriendsDni() != null && !request.getFriendsDni().isEmpty()) {
                int friendsAdded = addFriendRelationships(newUser, request.getFriendsDni());
                log.info("Added {} friend relationships for user with DNI: {}", friendsAdded, request.getDni());
                
                // Save the user again to persist the friend relationships
                userRepository.save(newUser);
            }

            return buildResponse(request.getDni(), "SUCCESS", 
                "User registered successfully with DNI: " + request.getDni());

        } catch (Exception e) {
            log.error("Error persisting user with DNI: {}", request.getDni(), e);
            return buildResponse(request.getDni(), "FAILED", 
                "Error persisting user: " + e.getMessage());
        }
    }

    private int addFriendRelationships(User user, List<Integer> friendsDni) {
        int friendsAdded = 0;
        List<Integer> notFoundDnis = new ArrayList<>();

        for (Integer friendDni : friendsDni) {
            userRepository.findByDni(friendDni).ifPresentOrElse(
                friend -> {
                    user.addFriend(friend);
                    log.debug("Added friend relationship: User {} -> Friend {}", user.getDni(), friend.getDni());
                },
                () -> {
                    notFoundDnis.add(friendDni);
                    log.warn("Friend with DNI {} not found in database", friendDni);
                }
            );
            friendsAdded++;
        }

        if (!notFoundDnis.isEmpty()) {
            log.warn("Could not add friends with DNIs {} - users not found", notFoundDnis);
        }

        return friendsAdded - notFoundDnis.size();
    }

    private PersistenceResponse buildResponse(Integer dni, String status, String message) {
        return PersistenceResponse.builder()
                .dni(dni)
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();
    }

    // Additional method to find a user by DNI (useful for testing and future features)
    @Transactional(readOnly = true)
    public User findByDni(Integer dni) {
        return userRepository.findByDniWithFriends(dni).orElse(null);
    }

    // Method to check if a user exists
    @Transactional(readOnly = true)
    public boolean userExists(Integer dni) {
        return userRepository.existsByDni(dni);
    }
} 