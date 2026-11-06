package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import jakarta.annotation.PostConstruct;

@Component
public class EntregaFallidaListener implements ListenerDonaciones<EventoEntregaFallida> {

    private final EventManagerDonaciones eventManager;
    private final RestTemplate restTemplate;
    private final RestDonacionesConfig config;

    public EntregaFallidaListener(EventManagerDonaciones eventManager,
            RestTemplate restTemplate, RestDonacionesConfig config) {
        this.eventManager = eventManager;
        this.restTemplate = restTemplate;
        this.config = config;
    }

    @PostConstruct
    public void register() {
        eventManager.registrarListener(EventoEntregaFallida.class, this);
    }

    @Override
    public void ejecutar(EventoEntregaFallida ev) {
        Donacion donacion = ev.getDonacion();
        Long donacionId = donacion != null ? donacion.getId() : null;
        String mensaje = "No se pudo realizar la entrega de la donación #" + donacionId
                + ". Motivo: " + ev.getMotivo();

        Donante donante = donacion != null ? donacion.getDonante() : null;
        if (donante != null) {
            enviarNotificacionAsync(donante.getContactoPredeterminado(), mensaje, "ENTREGA_FALLIDA", "DONANTE", donante.getId(), donacionId);
        }

        EntidadBeneficiaria entidad = donacion != null ? donacion.getEntidadBeneficiariaAsignada() : null;
        if (entidad != null) {
            for (MedioContacto contacto : entidad.getCorreoRepresentantes()) {
                enviarNotificacionAsync(contacto, mensaje, "ENTREGA_FALLIDA", "ENTIDAD", entidad.getId(), donacionId);
            }
        }

        // las personas administradoras revisan el caso y deciden si se replanifica la entrega
        String mensajeAdmin = mensaje
                + (entidad != null ? ". Entidad: " + entidad.getRazonSocial() : "")
                + ". Revisá el caso en el depósito para volver a planificarla.";
        for (String email : config.getAdminEmails()) {
            if (email != null && !email.isBlank()) {
                enviarNotificacionAsync(new MedioContacto(email.trim(), TipoContacto.EMAIL), mensajeAdmin,
                        "ENTREGA_FALLIDA", "ADMINISTRADOR", null, donacionId);
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
                System.err.println("Error enviando notificación asincrónica de entrega fallida: " + e.getMessage());
            }
        });
    }
}
