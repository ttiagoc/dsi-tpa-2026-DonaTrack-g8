package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Data;

@Data
public class NecesidadDTO {
    private Long id;
    private String subcategoria;
    private String descripcion;
    private Double cantidad;
    private Boolean estaSatisfecha;
    private Long entidadBeneficiariaId;
}
