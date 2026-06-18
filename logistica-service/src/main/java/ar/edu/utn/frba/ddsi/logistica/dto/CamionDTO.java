package ar.edu.utn.frba.ddsi.logistica.dto;

import lombok.Data;

@Data
public class CamionDTO {
    private Long id;
    private String patente;
    private Double capacidadVolumen;
    private Double altura;
    private Double capacidadCarga;
}
