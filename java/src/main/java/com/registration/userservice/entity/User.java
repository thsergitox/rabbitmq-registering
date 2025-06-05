package com.registration.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"friends", "friendOf"})
@ToString(exclude = {"friends", "friendOf"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nombre", length = 512)
    private String nombre;

    @Column(name = "correo", length = 512, unique = true)
    private String correo;

    @Column(name = "clave")
    private Integer clave;

    @Column(name = "dni", unique = true)
    private Integer dni;

    @Column(name = "telefono")
    private Integer telefono;

    @ManyToMany
    @JoinTable(
        name = "friend",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "friend_id")
    )
    @Builder.Default
    private Set<User> friends = new HashSet<>();

    @ManyToMany(mappedBy = "friends")
    @Builder.Default
    private Set<User> friendOf = new HashSet<>();

    public void addFriend(User friend) {
        friends.add(friend);
        friend.friendOf.add(this);
    }

    public void removeFriend(User friend) {
        friends.remove(friend);
        friend.friendOf.remove(this);
    }
} 