package ar.edu.utn.frba.ddsi.donaciones.services.notifiactions;

import lombok.Data;
import java.util.Map;

@Data
public class Evento {
    private TipoEvento tipo;
    private Map<String, Object> datos;
}
