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
public class EntregaExitosaListener implements ListenerDonaciones<EventoEntregaExitosa> {

    private final EventManagerDonaciones eventManager;
    private final RestTemplate restTemplate;
    private final RestDonacionesConfig config;

    public EntregaExitosaListener(EventManagerDonaciones eventManager,
            RestTemplate restTemplate, RestDonacionesConfig config) {
        this.eventManager = eventManager;
        this.restTemplate = restTemplate;
        this.config = config;
    }

    @PostConstruct
    public void register() {
        eventManager.registrarListener(EventoEntregaExitosa.class, this);
    }

    @Override
    public void ejecutar(EventoEntregaExitosa ev) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateFormatted = ev.getComprobante().getFechaHora().format(formatter);
        String patente = ev.getComprobante().getPatenteCamion();

        EntidadBeneficiaria entidad = ev.getEntidad();
        String mensajeEntidad = "Las donaciones fueron entregadas con éxito. Comprobante de entrega: "
                + "[Fecha/Hora: " + dateFormatted + " HS] "
                + "[Camión Responsable - Patente: " + patente + "].";
        for (MedioContacto contacto : entidad.getCorreoRepresentantes()) {
            enviarNotificacionAsync(contacto, mensajeEntidad);
        }

        for (Donacion donacion : ev.getDonaciones()) {
            Donante donante = donacion.getDonante();
            String mensajeDonante = "Tu donación fue entregada con éxito. Comprobante de entrega: "
                    + "[Fecha/Hora: " + dateFormatted + " HS] "
                    + "[Camión Responsable - Patente: " + patente + "].";
            enviarNotificacionAsync(donante.getContactoPredeterminado(), mensajeDonante);
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
                System.err.println("Error enviando notificación asincrónica de entrega exitosa: " + e.getMessage());
            }
        });
    }
}
