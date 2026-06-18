package ar.edu.utn.frba.ddsi.logistica.models;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class Ruta {
    private Long id;
    private LocalDate fecha;
    private String estado;
    private Camion camion;
    private String choferNombre;
}
