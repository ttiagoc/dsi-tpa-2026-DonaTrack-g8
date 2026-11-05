package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
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
        EntidadBeneficiaria entidad = ev.getEntidad();
        Donacion donacion = ev.getDonacion();
        Long donacionId = donacion != null ? donacion.getId() : null;

        if (donante != null) {
            String nombreEntidad = entidad != null ? entidad.getRazonSocial() : "una entidad beneficiaria";
            String mensajeDonante = "Tu donación" + (donacionId != null ? " #" + donacionId : "")
                    + " ha sido asignada a " + nombreEntidad + ".";
            enviarNotificacionAsync(
                    donante.getContactoPredeterminado(),
                    mensajeDonante,
                    "DONACION_ASIGNADA",
                    "DONANTE",
                    donante.getId(),
                    donacionId);
        }

        if (entidad != null) {
            String mensajeEntidad = "Se te ha asignado la donación" + (donacionId != null ? " #" + donacionId : "")
                    + " para cubrir tus necesidades.";
            for (MedioContacto contacto : entidad.getCorreoRepresentantes()) {
                enviarNotificacionAsync(
                        contacto,
                        mensajeEntidad,
                        "DONACION_ASIGNADA",
                        "ENTIDAD",
                        entidad.getId(),
                        donacionId);
            }
        }
    }

    private void enviarNotificacionAsync(MedioContacto contacto, String mensaje, String tipoEvento, String tipoDestinatario, Long destinatarioId, Long donacionId) {
        if (contacto == null) return;
        NotificacionRequest request = new NotificacionRequest(
                contacto.getValor(),
                contacto.getTipoContacto(),
                mensaje,
                tipoEvento,
                tipoDestinatario,
                destinatarioId,
                donacionId);
        String url = config.getNotificacionesUrl() + "/notificaciones";
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.postForObject(url, request, Void.class);
            } catch (Exception e) {
                System.err.println("Error enviando notificación asincrónica de asignación: " + e.getMessage());
            }
        });
    }
}
