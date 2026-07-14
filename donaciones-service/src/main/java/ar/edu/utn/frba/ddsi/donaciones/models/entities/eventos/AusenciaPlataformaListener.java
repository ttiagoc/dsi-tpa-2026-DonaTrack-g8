package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import java.util.concurrent.CompletableFuture;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;

@Component
public class AusenciaPlataformaListener implements ListenerDonaciones<EventoAusenciaPlataforma> {

    private final EventManagerDonaciones eventManager;
    private final RestTemplate restTemplate;
    private final RestDonacionesConfig config;

    public AusenciaPlataformaListener(EventManagerDonaciones eventManager,
            RestTemplate restTemplate, RestDonacionesConfig config) {
        this.eventManager = eventManager;
        this.restTemplate = restTemplate;
        this.config = config;
    }

    @PostConstruct
    public void register() {
        eventManager.registrarListener(EventoAusenciaPlataforma.class, this);
    }

    @Override
    public void ejecutar(EventoAusenciaPlataforma ev) {
        Donante donante = ev.getDonante();
        MedioContacto contacto = donante.getContactoPredeterminado();
        String mensaje = "¡Te extrañamos! Hace más de 20 días que no registrás actividad en DonaTrack. Tu ayuda hace la diferencia, sumate con una nueva donación.";
        enviarNotificacionAsync(contacto, mensaje);
    }

    private void enviarNotificacionAsync(MedioContacto contacto, String mensaje) {
        NotificacionRequest request = new NotificacionRequest(
                contacto.getValor(),
                contacto.getTipoContacto(),
                mensaje);
        String url = config.getNotificacionesUrl() + "/notificaciones";
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.postForObject(url, request, Void.class);
            } catch (Exception e) {
                System.err.println("Error enviando notificación asincrónica de ausencia: " + e.getMessage());
            }
        });
    }
}
