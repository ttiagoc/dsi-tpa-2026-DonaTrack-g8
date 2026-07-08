package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificacionesAdapter implements Listener {

    private final EventManager eventManager;
    private final RestTemplate restTemplate;
    private final RestDonacionesConfig config;

    public NotificacionesAdapter(EventManager eventManager, RestTemplate restTemplate, RestDonacionesConfig config) {
        this.eventManager = eventManager;
        this.restTemplate = restTemplate;
        this.config = config;
    }

    @PostConstruct
    public void registerListeners() {
        eventManager.registrarListener(EventoAusenciaPlataforma.class, this);
        eventManager.registrarListener(EventoDonacionAsignadaDonante.class, this);
        eventManager.registrarListener(EventoDonacionAsignadaEntidad.class, this);
        eventManager.registrarListener(EventoEntregaExitosaDonante.class, this);
        eventManager.registrarListener(EventoEntregaExitosaEntidad.class, this);
        eventManager.registrarListener(EventoEntregaFallida.class, this);
        eventManager.registrarListener(EventoInicioRutaDonante.class, this);
        eventManager.registrarListener(EventoInicioRutaEntidad.class, this);
    }

    @Override
    public void ejecutar(Evento evento) {
        if (evento == null)
            return;

        String mensaje = evento.getMensaje();
        MedioContacto contacto = evento.getContacto();

        if (contacto != null) {
            NotificacionRequest request = new NotificacionRequest(
                contacto.getValor(),
                contacto.getTipoContacto(),
                mensaje
            );
            String url = config.getNotificacionesUrl() + "/notificar";
            try {
                restTemplate.postForObject(url, request, Void.class);
            } catch (Exception e) {
                System.err.println("Error enviando notificación al microservicio: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Error: no se especificó un canal de contacto válido para el evento.");
        }
    }
}


