package ar.edu.utn.frba.ddsi.donaciones.models.entities.notifiaciones;

import lombok.Data;
import java.util.Map;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEvento;

@Data
public class Evento {
    private TipoEvento tipo;
    private Map<String, Object> datos;
}
