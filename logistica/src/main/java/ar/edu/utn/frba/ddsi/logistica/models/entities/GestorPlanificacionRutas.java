package ar.edu.utn.frba.ddsi.logistica.models.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.client.RestTemplate;
import ar.edu.utn.frba.ddsi.logistica.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.ResultadoPlanificacionDTO;

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

                ResultadoPlanificacionDTO mockResponse = new ResultadoPlanificacionDTO();
                List<CamionDTO> camionesProcesados = new ArrayList<>();
                List<Long> donacionesSobrantes = new ArrayList<>();

                Camion camionReal = camiones.get(0);
                CamionDTO camionDTO = new CamionDTO();
                camionDTO.setId(camionReal.getId());

                DireccionDTO paradaMock = new DireccionDTO();
                paradaMock.setDireccion("Medrano 951, CABA");

                List<Long> donacionesAceptadas = new ArrayList<>();

                for (int i = 0; i < donaciones.size(); i++) {
                    if (i < 80) {
                        donacionesAceptadas.add(donaciones.get(i).getId());
                    } else {
                        donacionesSobrantes.add(donaciones.get(i).getId());
                    }
                }

                paradaMock.setDonacionesIds(donacionesAceptadas);
                camionDTO.setDirecciones(List.of(paradaMock));
                camionesProcesados.add(camionDTO);

                mockResponse.setCamiones(camionesProcesados);
                mockResponse.setDonacionesSinAsignar(donacionesSobrantes);

                String urlCallback = "http://localhost:8081/api/planificacion/confirmacion";
                System.out.println("Cálculo terminado. Golpeando la URL de callback...");

                restTemplate.postForEntity(urlCallback, mockResponse, String.class);
            } catch (Exception e) {
                System.err.println("ERROR: Falló la simulación del componente externo: " + e.getMessage());
            }
        });
    }
}