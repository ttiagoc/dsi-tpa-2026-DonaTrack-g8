package ar.edu.utn.frba.ddsi.donaciones.models.entities.notifiaciones;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.Data;

@Data
public class NotificacionesAdapter implements Listener {
    @Override
    public void ejecutar(Evento evento) {
        String mensaje = this.armarMensaje(evento);

        MedioContacto contacto = (MedioContacto) evento.getDatos().get("contacto");

        if (contacto != null) {
            contacto.notificar(mensaje);
        } else {
            throw new RuntimeException("Error: no se especificó un canal de contacto válido para el evento.");
        }
    }

    private String armarMensaje(Evento evento) {
        return switch (evento.getTipo()) {
            case AUSENCIA_PLATAFORMA ->
                "¡Te extrañamos! Hace más de 20 días que no registrás actividad en DonaTrack. Tu ayuda hace la diferencia, sumate con una nueva donación.";

            case DONACION_ASIGNADA_ENTIDAD ->
                "Una donación te ha sido asignada.";

            case DONACION_ASIGNADA_DONANTE ->
                "Tu donación ha sido asignada.";

            case INICIO_RUTA_DONANTE ->
                "Tu donacion ya está en camino. Seguí el recorrido del camión en tiempo real haciendo click en el siguiente mapa interactivo: LINK";
            // TODO: Colocar el link real del mapa.

            case INICIO_RUTA_ENTIDAD ->
                "Tu entrega ya está en camino. Seguí el recorrido del camión en tiempo real haciendo click en el siguiente mapa interactivo: LINK";
            // TODO: Colocar el link real del mapa.

            case ENTREGA_EXITOSA_DONANTE ->
                "La entrega fue realizada con exito.";

            case ENTREGA_EXITOSA_ENTIDAD ->
                "La entrega fue realizada con exito.";

            case ENTREGA_FALLIDA ->
                "No se pudo realizar la entrega.";

            default -> throw new RuntimeException("Error: Tipo de evento no reconocido: " + evento.getTipo());
        };
    }
}
