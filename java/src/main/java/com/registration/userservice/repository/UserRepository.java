package com.registration.userservice.repository;

import com.registration.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByCorreo(String correo);

    Optional<User> findByDni(Integer dni);

    boolean existsByCorreo(String correo);

    boolean existsByDni(Integer dni);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.friends WHERE u.id = :id")
    Optional<User> findByIdWithFriends(@Param("id") Integer id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.friends WHERE u.dni = :dni")
    Optional<User> findByDniWithFriends(@Param("dni") Integer dni);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.friends WHERE u.dni IN :dnis")
    List<User> findByDniInWithFriends(@Param("dnis") List<Integer> dnis);

    @Query("SELECT u FROM User u WHERE u.dni IN :dnis")
    List<User> findByDniIn(@Param("dnis") List<Integer> dnis);
} 