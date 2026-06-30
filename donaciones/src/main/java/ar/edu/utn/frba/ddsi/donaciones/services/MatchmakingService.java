package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.enums.EstadoPropuesta;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEvento;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.MotorDeMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.notificaciones.EventManager;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.notificaciones.Evento;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.ResultadoMatchmakingRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MatchmakingService {

    private final DonacionRepository donacionRepository;
    private final EntidadBeneficiariaRepository entidadRepository;
    private final ResultadoMatchmakingRepository resultadoRepository;
    private final MotorDeMatchmaking motorDeMatchmaking;
    private final EventManager eventManager;

    @Scheduled(cron = "0 0 3 * * *")
    public void ejecutarProcesoNocturno() {
        System.out.println("Iniciando procesamiento automático de Matchmaking nocturno...");
        this.procesarMatchmakingGlobal();
    }

    public void procesarMatchmakingGlobal() {
        List<Donacion> donacionesEnDeposito = donacionRepository.buscarPorEstado(TipoEstadoDonacion.EN_DEPOSITO);

        List<EntidadBeneficiaria> todasLasEntidades = entidadRepository.findAll();

        if (donacionesEnDeposito.isEmpty() || todasLasEntidades.isEmpty()) {
            System.out.println("Matchmaking finalizado: No hay donaciones en depósito o entidades registradas.");
            return;
        }

        for (Donacion donacion : donacionesEnDeposito) {
            ResultadoMatchmaking resultado = motorDeMatchmaking.ejecutarMatchmaking(donacion, todasLasEntidades);
            resultadoRepository.save(resultado);
            System.out.println("Propuesta generada con éxito para la Donación ID: " + donacion.getId());
        }
    }

    public void aceptarPropuesta(Long propuestaId, Long entidadId) {
        ResultadoMatchmaking propuesta = resultadoRepository.findById(propuestaId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la propuesta con ID: " + propuestaId));

        EntidadBeneficiaria entidadElegida = propuesta.getEntidadesSugeridas().stream()
                .filter(e -> e.getId().equals(entidadId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
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
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la propuesta con ID: " + propuestaId));

        propuesta.setEstado(EstadoPropuesta.RECHAZADO);
        resultadoRepository.save(propuesta);
        System.out.println("Propuesta " + propuestaId + " RECHAZADA por el administrador.");
    }

    private void ejecutarNotificaciones(Donacion donacion, EntidadBeneficiaria entidad) {
        Map<String, Object> datosDonante = Map.of("contacto", donacion.getDonante().getContactoPredeterminado());
        Evento eventoAsignacionDonante = new Evento(TipoEvento.DONACION_ASIGNADA_DONANTE, datosDonante);
        eventManager.emitir(eventoAsignacionDonante);

        for (Email contacto : entidad.getCorreoRepresentantes()) {
            Map<String, Object> datosEntidad = Map.of("contacto", contacto);
            Evento eventoAsignacionEntidad = new Evento(TipoEvento.DONACION_ASIGNADA_ENTIDAD, datosEntidad);
            eventManager.emitir(eventoAsignacionEntidad);
        }

        Map<String, Object> datosEntidad = Map.of("contacto", entidad.getTelefono());
        Evento eventoAsignacionEntidad = new Evento(TipoEvento.DONACION_ASIGNADA_ENTIDAD, datosEntidad);
        eventManager.emitir(eventoAsignacionEntidad);
    }

}