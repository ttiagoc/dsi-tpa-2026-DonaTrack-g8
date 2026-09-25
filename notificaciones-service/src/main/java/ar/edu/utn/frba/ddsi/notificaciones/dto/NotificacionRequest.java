package ar.edu.utn.frba.ddsi.notificaciones.dto;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionRequest {
    private String valor;
    private TipoContacto tipoContacto;
    private String mensaje;
    private String tipoEvento;
    private Long destinatarioId;
    private Long donacionId;

    public NotificacionRequest(String valor, TipoContacto tipoContacto, String mensaje) {
        this(valor, tipoContacto, mensaje, null, null, null);
    }
}
