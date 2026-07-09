package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.util.List;

import lombok.Data;

@Data
public class Parada {
    private Integer orden;
    private String destino;
    private Long entidadId;
    private List<Long> donacionIds;
}
