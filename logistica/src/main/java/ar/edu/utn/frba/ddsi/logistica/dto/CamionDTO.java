package ar.edu.utn.frba.ddsi.logistica.dto;

import java.util.List;

import lombok.Data;

@Data
public class CamionDTO {
    private Long id;
    private List<DireccionDTO> direcciones;
}
