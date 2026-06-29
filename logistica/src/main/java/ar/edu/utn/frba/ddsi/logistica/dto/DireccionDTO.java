package ar.edu.utn.frba.ddsi.logistica.dto;

import java.util.List;

import lombok.Data;

@Data
public class DireccionDTO {
    private String direccion;
    private List<Long> donacionesIds;
}
