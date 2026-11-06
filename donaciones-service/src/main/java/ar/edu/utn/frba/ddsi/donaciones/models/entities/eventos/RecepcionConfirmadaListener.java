package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import java.time.format.DateTimeFormatter;
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
public class RecepcionConfirmadaListener implements ListenerDonaciones<EventoRecepcionConfirmada> {

    private final EventManagerDonaciones eventManager;
    private final RestTemplate restTemplate;
    private final RestDonacionesConfig config;

    public RecepcionConfirmadaListener(EventManagerDonaciones eventManager,
            RestTemplate restTemplate, RestDonacionesConfig config) {
        this.eventManager = eventManager;
        this.restTemplate = restTemplate;
        this.config = config;
    }

    @PostConstruct
    public void register() {
        eventManager.registrarListener(EventoRecepcionConfirmada.class, this);
    }

    @Override
    public void ejecutar(EventoRecepcionConfirmada ev) {
        Donacion donacion = ev.getDonacion();
        Donante donante = ev.getDonante();
        EntidadBeneficiaria entidad = ev.getEntidad();
        Long donacionId = donacion != null ? donacion.getId() : null;
        String comprobante = comprobanteDe(donacion);

        if (donante != null) {
            String nombreEntidad = entidad != null ? entidad.getRazonSocial() : "la entidad beneficiaria";
            String mensajeDonante = "¡Tu donación #" + donacionId + " fue recibida por " + nombreEntidad
                    + "! Ya podés ver las fotos de la recepción en la plataforma." + comprobante;
            enviarNotificacionAsync(
                    donante.getContactoPredeterminado(),
                    mensajeDonante,
                    "RECEPCION_CONFIRMADA",
                    "DONANTE",
                    donante.getId(),
                    donacionId);
        }

        if (entidad != null) {
            String mensajeEntidad = "Confirmaste la recepción de la donación #" + donacionId + " con éxito." + comprobante;
            for (MedioContacto contacto : entidad.getCorreoRepresentantes()) {
                enviarNotificacionAsync(
                        contacto,
                        mensajeEntidad,
                        "RECEPCION_CONFIRMADA",
                        "ENTIDAD",
                        entidad.getId(),
                        donacionId);
            }
        }
    }

    // consigna: la confirmacion lleva el comprobante de entrega (fecha, hora y camion responsable)
    private static String comprobanteDe(Donacion donacion) {
        if (donacion == null || donacion.fechaDelEstadoActual() == null) {
            return "";
        }
        String fechaHora = donacion.fechaDelEstadoActual().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String patente = donacion.patenteCamionDeLaEntrega();
        return " Comprobante de entrega: [Fecha/Hora: " + fechaHora + " HS]"
                + (patente != null ? " [Camión Responsable - Patente: " + patente + "]" : "") + ".";
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
                System.err.println("Error enviando notificación asincrónica de recepción confirmada: " + e.getMessage());
            }
        });
    }
}

