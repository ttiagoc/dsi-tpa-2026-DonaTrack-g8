package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.PropuestaMatchmakingResponse;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventManagerDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoDonacionAsignada;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoPropuesta;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.ResultadoMatchmakingRepository;

@Component
public class MotorDeMatchmaking {

    private final List<AlgoritmoAsignacion> algoritmos;
    private final DonacionRepository donacionRepository;
    private final EntidadBeneficiariaRepository entidadRepository;
    private final ResultadoMatchmakingRepository resultadoRepository;
    private final EventManagerDonaciones eventManager;

    public MotorDeMatchmaking(List<AlgoritmoAsignacion> algoritmos,
                             DonacionRepository donacionRepository,
                             EntidadBeneficiariaRepository entidadRepository,
                             ResultadoMatchmakingRepository resultadoRepository,
                             EventManagerDonaciones eventManager) {
        this.algoritmos = algoritmos;
        this.donacionRepository = donacionRepository;
        this.entidadRepository = entidadRepository;
        this.resultadoRepository = resultadoRepository;
        this.eventManager = eventManager;
    }

    public List<AlgoritmoAsignacion> getAlgoritmos() {
        return this.algoritmos;
    }

    private ResultadoMatchmaking ejecutarMatchmaking(Donacion donacion, List<EntidadBeneficiaria> entidadesCandidatas) {
        if (algoritmos.isEmpty()) {
            return new ResultadoMatchmaking(donacion, new ArrayList<>());
        }

        List<List<EntidadBeneficiaria>> resultadosRankings = algoritmos.stream()
                .map(algoritmo -> algoritmo.generarRanking(donacion, entidadesCandidatas))
                .toList();

        List<EntidadBeneficiaria> rankingBase = resultadosRankings.get(0);
        List<EntidadBeneficiaria> coincidenciaInterseccion = new ArrayList<>(rankingBase);

        for (int i = 1; i < resultadosRankings.size(); i++) {
            coincidenciaInterseccion.retainAll(resultadosRankings.get(i));
        }

        List<EntidadBeneficiaria> sugerenciasFinales;
        if (!coincidenciaInterseccion.isEmpty()) {
            sugerenciasFinales = coincidenciaInterseccion;
        } else {
            List<EntidadBeneficiaria> todasLasSugeridas = new ArrayList<>();
            for (List<EntidadBeneficiaria> ranking : resultadosRankings) {
                for (EntidadBeneficiaria e : ranking) {
                    if (!todasLasSugeridas.contains(e)) {
                        todasLasSugeridas.add(e);
                    }
                }
            }
            sugerenciasFinales = todasLasSugeridas;
        }

        return new ResultadoMatchmaking(donacion, sugerenciasFinales);
    }

    public List<PropuestaMatchmakingResponse> obtenerPropuestasPendientes() {
        List<ResultadoMatchmaking> pendientes = resultadoRepository.buscarPendientes();
        return pendientes.stream()
                .map(this::toPropuestaMatchmakingResponse)
                .toList();
    }

    private PropuestaMatchmakingResponse toPropuestaMatchmakingResponse(ResultadoMatchmaking p) {
        return new PropuestaMatchmakingResponse(
                p.getId(),
                p.getDonacion().getId(),
                p.getEntidadesSugeridas().stream().map(EntidadBeneficiaria::getId).toList(),
                p.getFechaEjecucion());
    }

    public void procesarMatchmaking() {
        System.out.println("Iniciando procesamiento automático de Matchmaking nocturno...");

        List<Donacion> donacionesEnDeposito = donacionRepository.buscarPorEstado(TipoEstadoDonacion.EN_DEPOSITO);
        List<EntidadBeneficiaria> todasLasEntidades = entidadRepository.findAll();

        if (donacionesEnDeposito.isEmpty() || todasLasEntidades.isEmpty()) {
            System.out.println("Matchmaking finalizado: No hay donaciones en depósito o entidades registradas.");
            return;
        }

        for (Donacion donacion : donacionesEnDeposito) {
            ResultadoMatchmaking resultado = ejecutarMatchmaking(donacion, todasLasEntidades);
            resultadoRepository.save(resultado);
            System.out.println("Propuesta generada con éxito para la Donación ID: " + donacion.getId());
        }
    }

    public void aceptarPropuesta(Long propuestaId, Long entidadId) {
        ResultadoMatchmaking propuesta = resultadoRepository.findById(propuestaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una propuesta con el id: " + propuestaId));

        EntidadBeneficiaria entidadElegida = propuesta.getEntidadesSugeridas().stream()
                .filter(e -> e.getId().equals(entidadId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La entidad elegida no forma parte de las sugerencias válidas"));

        propuesta.setEstado(EstadoPropuesta.ACEPTADO);
        resultadoRepository.save(propuesta);

        Donacion donacion = propuesta.getDonacion();
        donacion.cambiarEstado(TipoEstadoDonacion.ASIGNACION_REALIZADA,
                "Donación asignada a la entidad: " + entidadElegida.getId());
        donacionRepository.save(donacion);

        ejecutarNotificaciones(donacion, entidadElegida);

        entidadElegida.getNecesidades().stream()
                .filter(n -> n.getSubcategoria().equals(donacion.getSubcategoria()))
                .findFirst()
                .ifPresent(n -> n.asignarDonacion(donacion));

        entidadRepository.save(entidadElegida);
        System.out.println(
                "Propuesta " + propuestaId + " ACEPTADA. Donación asignada a la entidad: " + entidadElegida.getId());
    }

    public void rechazarPropuesta(Long propuestaId) {
        ResultadoMatchmaking propuesta = resultadoRepository.findById(propuestaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una propuesta con el id: " + propuestaId));

        propuesta.setEstado(EstadoPropuesta.RECHAZADO);
        resultadoRepository.save(propuesta);
        System.out.println("Propuesta " + propuestaId + " RECHAZADA por el administrador.");
    }

    private void ejecutarNotificaciones(Donacion donacion, EntidadBeneficiaria entidad) {
        eventManager
                .emitir(new EventoDonacionAsignada(donacion.getDonante(), entidad));
    }
}