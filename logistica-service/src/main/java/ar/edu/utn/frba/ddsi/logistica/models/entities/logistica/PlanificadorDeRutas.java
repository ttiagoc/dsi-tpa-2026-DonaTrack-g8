package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.CamionPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.DireccionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.grpc.DonacionesGrpcClient;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;

@Component
public class PlanificadorDeRutas {

    private final DonacionesGrpcClient donacionesGrpcClient;
    private final GestorPlanificacionRutas gestorPlanificacionRutas;
    private final CamionRepository camionRepository;
    private final RutaRepository rutaRepository;
    private static final int TAMANO_LOTE_DONACIONES = 100;
    private static final int TAMANO_LOTE_CAMIONES = 5;

    public PlanificadorDeRutas(DonacionesGrpcClient donacionesGrpcClient,
            GestorPlanificacionRutas gestorPlanificacionRutas, CamionRepository camionRepository,
            RutaRepository rutaRepository) {
        this.donacionesGrpcClient = donacionesGrpcClient;
        this.gestorPlanificacionRutas = gestorPlanificacionRutas;
        this.camionRepository = camionRepository;
        this.rutaRepository = rutaRepository;
    }

    public void planificarRutas() {
        List<Camion> camionesDisponibles = camionRepository.findAllDisponibles();
        if (camionesDisponibles.isEmpty()) {
            return;
        }

        int offset = 0;
        int camionIndex = 0;

        while (true) {
            if (camionIndex >= camionesDisponibles.size()) {
                break;
            }

            List<DonacionDTO> loteDonaciones = getLote(offset);
            if (loteDonaciones.isEmpty()) {
                break;
            }

            int toIndex = Math.min(camionIndex + TAMANO_LOTE_CAMIONES, camionesDisponibles.size());
            List<Camion> loteCamiones = camionesDisponibles.subList(camionIndex, toIndex);

            gestorPlanificacionRutas.solicitarPlanificacion(loteDonaciones, loteCamiones);

            offset += TAMANO_LOTE_DONACIONES;
            camionIndex += TAMANO_LOTE_CAMIONES;
        }
    }

    public void ejecutarPlanificacion(EjecutarPlanificacionRequest request) {
        if (request == null) {
            return;
        }

        // 1. Persistir las rutas y sus paradas asignadas a cada camión
        if (request.camiones() != null) {
            for (CamionPlanificacionRequest camionReq : request.camiones()) {
                Optional<Camion> camionOpt = camionRepository.findById(camionReq.id());
                if (camionOpt.isEmpty()) {
                    System.err.println("No se encontró el camión con id: " + camionReq.id() + ". Se omite la ruta.");
                    continue;
                }

                Camion camion = camionOpt.get();
                List<Parada> paradas = new ArrayList<>();
                int orden = 1;

                if (camionReq.direcciones() != null) {
                    for (DireccionRequest dirReq : camionReq.direcciones()) {
                        Parada parada = new Parada();
                        parada.setOrden(orden++);
                        parada.setDestino(dirReq.direccion());
                        parada.setEntidadId(dirReq.entidadId());
                        parada.setDonacionIds(dirReq.donacionesIds() != null
                                ? new ArrayList<>(dirReq.donacionesIds())
                                : new ArrayList<>());
                        paradas.add(parada);
                    }
                }

                Ruta nuevaRuta = new Ruta(LocalDate.now().plusDays(1), camion, paradas);
                rutaRepository.save(nuevaRuta);
            }
        }

        // 2. Notificar cambio de estado a LISTA_PARA_ENTREGAR vía gRPC para las donaciones asignadas
        List<Long> donacionesPlanificadas = toListIdDonaciones(request.camiones());
        for (Long donacionId : donacionesPlanificadas) {
            donacionesGrpcClient.actualizarEstado(
                    donacionId,
                    "LISTA_PARA_ENTREGAR",
                    "Donacion lista para entregar");
        }

        // 3. Gestionar donaciones sin asignar devueltas por el planificador externo
        if (request.donacionesSinAsignar() != null && !request.donacionesSinAsignar().isEmpty()) {
            System.out.println("Donaciones devueltas sin asignar por el componente externo: "
                    + request.donacionesSinAsignar()
                    + ". Permanecen en estado ASIGNACION_REALIZADA para el próximo ciclo de planificación.");
        }
    }

    private List<DonacionDTO> getLote(int offset) {
        return donacionesGrpcClient.listarPorEstado("asignacion_realizada", TAMANO_LOTE_DONACIONES, offset);
    }

    private List<Long> toListIdDonaciones(List<CamionPlanificacionRequest> camiones) {
        return camiones.stream()
                .flatMap(c -> c.direcciones().stream())
                .flatMap(d -> d.donacionesIds().stream())
                .collect(Collectors.toList());
    }
}
