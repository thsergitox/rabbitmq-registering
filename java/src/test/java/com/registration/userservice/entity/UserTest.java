package com.registration.userservice.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Should create user with builder")
    void shouldCreateUserWithBuilder() {
        // Given
        Integer id = 1;
        String nombre = "Juan Pérez";
        String correo = "juan.perez@example.com";
        Integer clave = 1234;
        Integer dni = 12345678;
        Integer telefono = 987654321;

        // When
        User user = User.builder()
                .id(id)
                .nombre(nombre)
                .correo(correo)
                .clave(clave)
                .dni(dni)
                .telefono(telefono)
                .build();

        // Then
        assertEquals(id, user.getId());
        assertEquals(nombre, user.getNombre());
        assertEquals(correo, user.getCorreo());
        assertEquals(clave, user.getClave());
        assertEquals(dni, user.getDni());
        assertEquals(telefono, user.getTelefono());
        assertNotNull(user.getFriends());
        assertNotNull(user.getFriendOf());
        assertTrue(user.getFriends().isEmpty());
        assertTrue(user.getFriendOf().isEmpty());
    }

    @Test
    @DisplayName("Should add friend relationship")
    void shouldAddFriendRelationship() {
        // Given
        User user1 = User.builder()
                .id(1)
                .nombre("User 1")
                .dni(11111111)
                .build();
        
        User user2 = User.builder()
                .id(2)
                .nombre("User 2")
                .dni(22222222)
                .build();

        // When
        user1.addFriend(user2);

        // Then
        assertTrue(user1.getFriends().contains(user2));
        assertTrue(user2.getFriendOf().contains(user1));
        assertEquals(1, user1.getFriends().size());
        assertEquals(1, user2.getFriendOf().size());
    }

    @Test
    @DisplayName("Should remove friend relationship")
    void shouldRemoveFriendRelationship() {
        // Given
        User user1 = User.builder()
                .id(1)
                .nombre("User 1")
                .dni(11111111)
                .build();
        
        User user2 = User.builder()
                .id(2)
                .nombre("User 2")
                .dni(22222222)
                .build();
        
        user1.addFriend(user2);

        // When
        user1.removeFriend(user2);

        // Then
        assertFalse(user1.getFriends().contains(user2));
        assertFalse(user2.getFriendOf().contains(user1));
        assertTrue(user1.getFriends().isEmpty());
        assertTrue(user2.getFriendOf().isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple friend relationships")
    void shouldHandleMultipleFriendRelationships() {
        // Given
        User user1 = User.builder().id(1).nombre("User 1").dni(11111111).build();
        User user2 = User.builder().id(2).nombre("User 2").dni(22222222).build();
        User user3 = User.builder().id(3).nombre("User 3").dni(33333333).build();
        User user4 = User.builder().id(4).nombre("User 4").dni(44444444).build();

        // When
        user1.addFriend(user2);
        user1.addFriend(user3);
        user2.addFriend(user4);

        // Then
        assertEquals(2, user1.getFriends().size());
        assertTrue(user1.getFriends().contains(user2));
        assertTrue(user1.getFriends().contains(user3));
        
        assertEquals(1, user2.getFriends().size());
        assertTrue(user2.getFriends().contains(user4));
        
        assertEquals(1, user2.getFriendOf().size());
        assertTrue(user2.getFriendOf().contains(user1));
        
        assertEquals(1, user3.getFriendOf().size());
        assertTrue(user3.getFriendOf().contains(user1));
        
        assertEquals(1, user4.getFriendOf().size());
        assertTrue(user4.getFriendOf().contains(user2));
    }

    @Test
    @DisplayName("Should handle null values in optional fields")
    void shouldHandleNullValues() {
        // Given & When
        User user = User.builder()
                .id(1)
                .dni(12345678)
                .build();

        // Then
        assertNull(user.getNombre());
        assertNull(user.getCorreo());
        assertNull(user.getClave());
        assertNull(user.getTelefono());
        assertNotNull(user.getFriends());
        assertNotNull(user.getFriendOf());
    }

    @Test
    @DisplayName("Should not include friends in equals and toString")
    void shouldNotIncludeFriendsInEqualsAndToString() {
        // Given
        User user1 = User.builder().id(1).nombre("User 1").dni(11111111).build();
        User user2 = User.builder().id(1).nombre("User 1").dni(11111111).build();
        User user3 = User.builder().id(3).nombre("User 3").dni(33333333).build();

        // When
        user1.addFriend(user3);

        // Then
        assertEquals(user1, user2); // Should be equal despite different friends
        assertFalse(user1.toString().contains("friends")); // toString should not include friends
        assertFalse(user1.toString().contains("friendOf")); // toString should not include friendOf
    }
} 