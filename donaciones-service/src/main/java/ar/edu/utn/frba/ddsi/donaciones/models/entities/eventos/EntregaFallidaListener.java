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

        String mensaje = "No se pudo realizar la entrega de la donación #" + donacion.getId()
                + ". Motivo: " + ev.getMotivo();

        Donante donante = donacion.getDonante();
        enviarNotificacionAsync(donante.getContactoPredeterminado(), mensaje);

        EntidadBeneficiaria entidad = donacion.getEntidadBeneficiariaAsignada();
        for (MedioContacto contacto : entidad.getCorreoRepresentantes()) {
            enviarNotificacionAsync(contacto, mensaje);
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
                System.err.println("Error enviando notificación asincrónica de entrega fallida: " + e.getMessage());
            }
        });
    }
}
