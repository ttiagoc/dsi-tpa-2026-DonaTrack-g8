package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.grpc.DonacionesGrpcClient;
import ar.edu.utn.frba.ddsi.logistica.models.entities.eventos.EventManagerLogistica;
import ar.edu.utn.frba.ddsi.logistica.models.entities.eventos.EventoInicioRuta;

@Component
public class GestorDeRutas {

    private final DonacionesGrpcClient donacionesGrpcClient;
    private final RestLogisticaConfig properties;
    private final EventManagerLogistica eventManager;

    public GestorDeRutas(DonacionesGrpcClient donacionesGrpcClient,
            RestLogisticaConfig properties, EventManagerLogistica eventManager) {
        this.donacionesGrpcClient = donacionesGrpcClient;
        this.properties = properties;
        this.eventManager = eventManager;
    }

    public void iniciarRuta(Ruta ruta) {
        for (Parada parada : ruta.getParadas()) {
            for (Long donacionId : parada.getDonacionIds()) {
                donacionesGrpcClient.actualizarEstado(
                        donacionId,
                        "EN_TRASLADO",
                        "La donación se encuentra en camino a su destino.");
            }
        }

        String baseWebUrl = properties.getWebUrl().replaceAll("/+$", "");
        eventManager.emitir(
                new EventoInicioRuta(ruta, baseWebUrl + "/seguimiento"));
    }

    public void confirmarEntregaExitosa(Parada paradaAfectada, Ruta ruta) {
        donacionesGrpcClient.confirmarEntrega(
                paradaAfectada.getEntidadId(),
                paradaAfectada.getDonacionIds(),
                ruta.getCamion().getPatente(),
                LocalDateTime.now());
        System.out.println("Reporte de entrega exitosa transmitido a Donaciones vía gRPC para Parada ID: " + paradaAfectada.getOrden());
    }

    public void registrarEntregaFallida(Parada paradaAfectada, String motivo) {
        String justificacion = (motivo != null && !motivo.isBlank())
                ? motivo
                : "Entrega no recibida en la parada #" + paradaAfectada.getOrden();
        if (paradaAfectada.getDonacionIds() == null) {
            return;
        }
        for (Long donacionId : paradaAfectada.getDonacionIds()) {
            donacionesGrpcClient.actualizarEstado(
                    donacionId,
                    "ENTREGA_FALLIDA",
                    justificacion);
        }
    }
}
