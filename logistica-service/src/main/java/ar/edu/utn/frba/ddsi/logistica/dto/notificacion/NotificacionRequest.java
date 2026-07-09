package ar.edu.utn.frba.ddsi.logistica.dto.notificacion;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionRequest {
    private String valor;
    private TipoContacto tipoContacto;
    private String mensaje;
}
