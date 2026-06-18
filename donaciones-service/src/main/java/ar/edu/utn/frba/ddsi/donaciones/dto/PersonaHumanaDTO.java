package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PersonaHumanaDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private LocalDate fechaNacimiento;
    private String dni;
    private String genero;
    private String direccion;
    private List<String> emailsContacto;
}
