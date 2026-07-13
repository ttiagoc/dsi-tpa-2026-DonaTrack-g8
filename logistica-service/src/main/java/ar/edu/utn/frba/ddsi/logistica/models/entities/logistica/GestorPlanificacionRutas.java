package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.CamionPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.DireccionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;

@Component
public class GestorPlanificacionRutas {

    private final RestTemplate restTemplate;

    public GestorPlanificacionRutas(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void solicitarPlanificacion(List<DonacionDTO> donaciones, List<Camion> camiones) {
        System.out.println("Solicitud recibida. Calculando rutas asincrónicamente...");

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000);

                List<Long> donacionesSobrantes = new ArrayList<>();
                List<Long> donacionesAceptadas = new ArrayList<>();

                for (int i = 0; i < donaciones.size(); i++) {
                    if (i < 80) {
                        donacionesAceptadas.add(donaciones.get(i).getId());
                    } else {
                        donacionesSobrantes.add(donaciones.get(i).getId());
                    }
                }

                DireccionRequest direccionInfo = new DireccionRequest("Medrano 951, CABA", donacionesAceptadas);

                Camion camion = camiones.get(0);
                CamionPlanificacionRequest camionPlanif = new CamionPlanificacionRequest(camion.getId(),
                        List.of(direccionInfo));

                List<CamionPlanificacionRequest> camionesProcesados = new ArrayList<>();
                camionesProcesados.add(camionPlanif);

                EjecutarPlanificacionRequest mockResponse = new EjecutarPlanificacionRequest(camionesProcesados,
                        donacionesSobrantes);

                String urlCallback = "http://localhost:8081/api/rutas/planificaciones/callback";
                System.out.println("Cálculo terminado. Golpeando la URL de callback...");

                restTemplate.postForObject(urlCallback, mockResponse, Void.class);
            } catch (Exception e) {
                System.err.println("ERROR: Falló la simulación del componente externo: " + e.getMessage());
            }
        });
    }
}