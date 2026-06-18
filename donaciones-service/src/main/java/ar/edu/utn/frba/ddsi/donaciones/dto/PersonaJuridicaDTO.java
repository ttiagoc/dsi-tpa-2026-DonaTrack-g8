package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Data;

import java.util.List;

@Data
public class PersonaJuridicaDTO {
    private Long id;
    private String razonSocial;
    private String rubro;
    private String tipo;
    private String cuit;
    private List<String> emailsContacto;
}
