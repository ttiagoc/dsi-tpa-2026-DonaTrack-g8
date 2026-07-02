package ar.edu.utn.frba.ddsi.logistica.models.entities;

import lombok.Data;

@Data
public class Camion {
    private Long id;
    private String patente;
    private Double capacidadVolumen;
    private Double altura;
    private Double capacidadCarga;
    private Chofer chofer;
}
