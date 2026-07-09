package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import jakarta.annotation.PostConstruct;

@Component
public class DonacionAsignadaListener implements ListenerDonaciones<EventoDonacionAsignada> {

    private final EventManagerDonaciones eventManager;
    private final RestTemplate restTemplate;
    private final RestDonacionesConfig config;

    public DonacionAsignadaListener(EventManagerDonaciones eventManager,
            RestTemplate restTemplate, RestDonacionesConfig config) {
        this.eventManager = eventManager;
        this.restTemplate = restTemplate;
        this.config = config;
    }

    @PostConstruct
    public void register() {
        eventManager.registrarListener(EventoDonacionAsignada.class, this);
    }

    @Override
    public void ejecutar(EventoDonacionAsignada ev) {
        Donante donante = ev.getDonante();
        enviarNotificacionAsync(donante.getContactoPredeterminado(), "Tu donación ha sido asignada.");

        EntidadBeneficiaria entidad = ev.getEntidad();
        for (MedioContacto contacto : entidad.getCorreoRepresentantes()) {
            enviarNotificacionAsync(contacto, "Una donación te ha sido asignada.");
        }
    }

    private void enviarNotificacionAsync(MedioContacto contacto, String mensaje) {
        NotificacionRequest request = new NotificacionRequest(
                contacto.getValor(),
                contacto.getTipoContacto(),
                mensaje);
        String url = config.getNotificacionesUrl() + "/notificar";
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.postForObject(url, request, Void.class);
            } catch (Exception e) {
                System.err.println("Error enviando notificación asincrónica de asignación: " + e.getMessage());
            }
        });
    }
}
