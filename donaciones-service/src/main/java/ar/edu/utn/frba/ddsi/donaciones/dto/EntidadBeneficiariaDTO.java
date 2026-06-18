package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Data;

import java.util.List;

@Data
public class EntidadBeneficiariaDTO {
    private Long id;
    private String razonSocial;
    private String direccion;
    private String telefono;
    private List<String> correoRepresentantes;
}
