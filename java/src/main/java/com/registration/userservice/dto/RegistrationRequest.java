package com.registration.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {
    private String nombre;
    private String correo;
    private Integer clave;
    private Integer dni;
    private Integer telefono;
    private List<Integer> friendsDni;
} 