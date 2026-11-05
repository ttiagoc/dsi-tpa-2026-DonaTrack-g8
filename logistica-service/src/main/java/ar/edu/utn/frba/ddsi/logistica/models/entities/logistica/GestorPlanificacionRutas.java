package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.CamionPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.DireccionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;

@Component
public class GestorPlanificacionRutas {

    private final RestTemplate restTemplate;
    private final String callbackUrl;

    @Autowired
    public GestorPlanificacionRutas(
            RestTemplate restTemplate,
            @Value("${logistica.callback-url:http://localhost:8081/api/rutas/planificaciones/resultados}") String callbackUrl) {
        this.restTemplate = restTemplate;
        this.callbackUrl = callbackUrl;
    }

    public GestorPlanificacionRutas(RestTemplate restTemplate) {
        this(restTemplate, "http://localhost:8081/api/rutas/planificaciones/resultados");
    }

    public void solicitarPlanificacion(List<DonacionDTO> donaciones, List<Camion> camiones) {
        if (donaciones == null || donaciones.isEmpty() || camiones == null || camiones.isEmpty()) {
            return;
        }

        EjecutarPlanificacionRequest resultado = calcularPlanificacion(donaciones, camiones);

        CompletableFuture<Void> tareaCallback = CompletableFuture.runAsync(() -> {
            try {
                restTemplate.postForObject(this.callbackUrl, resultado, Void.class);
            } catch (Exception e) {
                System.err.println("ERROR: Falló el envío del callback de planificación: " + e.getMessage());
            }
        });

        try {
            tareaCallback.get(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Si el callback demora más, continúa en segundo plano sin bloquear indefinidamente
        }
    }

    public EjecutarPlanificacionRequest calcularPlanificacion(List<DonacionDTO> donaciones, List<Camion> camiones) {
        int totalCamiones = camiones.size();
        double[] pesoRestante = new double[totalCamiones];
        double[] volumenRestante = new double[totalCamiones];
        List<Map<String, ParadaAgrupada>> paradasPorCamion = new ArrayList<>(totalCamiones);

        for (int i = 0; i < totalCamiones; i++) {
            Camion c = camiones.get(i);
            pesoRestante[i] = c.getCapacidadCarga() != null ? c.getCapacidadCarga() : Double.MAX_VALUE;
            volumenRestante[i] = c.getCapacidadVolumen() != null ? c.getCapacidadVolumen() : Double.MAX_VALUE;
            paradasPorCamion.add(new LinkedHashMap<>());
        }

        List<Long> donacionesSobrantes = new ArrayList<>();

        for (DonacionDTO d : donaciones) {
            double peso = d.getPeso() != null ? d.getPeso() : 0.0;
            double volumen = d.getVolumen() != null ? d.getVolumen() : 0.0;
            String direccion = (d.getDireccion() != null && !d.getDireccion().isBlank())
                    ? d.getDireccion()
                    : GeocodificadorCaba.DEPOSITO_DIRECCION;
            String claveParada = (d.getEntidadId() != null ? d.getEntidadId() : "dir") + "|" + direccion;

            int elegido = -1;
            // Priorizar camión que ya visita la misma entidad/dirección si tiene capacidad
            for (int i = 0; i < totalCamiones; i++) {
                if (paradasPorCamion.get(i).containsKey(claveParada)
                        && peso <= pesoRestante[i]
                        && volumen <= volumenRestante[i]) {
                    elegido = i;
                    break;
                }
            }
            // Si ninguno la visita aún, elegir el primer camión con capacidad disponible
            if (elegido < 0) {
                for (int i = 0; i < totalCamiones; i++) {
                    if (peso <= pesoRestante[i] && volumen <= volumenRestante[i]) {
                        elegido = i;
                        break;
                    }
                }
            }

            if (elegido >= 0) {
                pesoRestante[elegido] -= peso;
                volumenRestante[elegido] -= volumen;
                ParadaAgrupada grupo = paradasPorCamion.get(elegido).computeIfAbsent(
                        claveParada,
                        k -> new ParadaAgrupada(direccion, d.getEntidadId()));
                grupo.donacionIds.add(d.getId());
            } else if (d.getId() != null) {
                donacionesSobrantes.add(d.getId());
            }
        }

        List<CamionPlanificacionRequest> camionesProcesados = new ArrayList<>();
        for (int i = 0; i < totalCamiones; i++) {
            Map<String, ParadaAgrupada> paradas = paradasPorCamion.get(i);
            if (!paradas.isEmpty()) {
                List<DireccionRequest> direcciones = paradas.values().stream()
                        .map(g -> new DireccionRequest(g.direccion, g.donacionIds, g.entidadId))
                        .toList();
                camionesProcesados.add(new CamionPlanificacionRequest(camiones.get(i).getId(), direcciones));
            }
        }

        return new EjecutarPlanificacionRequest(camionesProcesados, donacionesSobrantes);
    }

    private static final class ParadaAgrupada {
        final String direccion;
        final Long entidadId;
        final List<Long> donacionIds = new ArrayList<>();

        ParadaAgrupada(String direccion, Long entidadId) {
            this.direccion = Objects.requireNonNullElse(direccion, GeocodificadorCaba.DEPOSITO_DIRECCION);
            this.entidadId = entidadId;
        }
    }
}
