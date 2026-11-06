package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.InicioRutaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ParadaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

@Component
public class GestorDeEventos {

    // consigna: se avisa al donante que no registra interaccion durante mas de 20 dias
    private static final int DIAS_INACTIVIDAD = 20;

    private final EventManagerDonaciones eventManager;
    private final DonacionRepository donacionRepository;
    private final DonanteRepository donanteRepository;

    public GestorDeEventos(EventManagerDonaciones eventManager, DonacionRepository donacionRepository,
            DonanteRepository donanteRepository) {
        this.eventManager = eventManager;
        this.donacionRepository = donacionRepository;
        this.donanteRepository = donanteRepository;
    }

    public void notificarAusenciaDonante(Donante donante) {
        EventoDonaciones eventoAusencia = new EventoAusenciaPlataforma(donante);
        eventManager.emitir(eventoAusencia);
    }

    // A demanda (el administrador): avisa a todos los donantes que llevan mas de 20 dias sin donar.
    public void verificarInactividadDonantes() {
        verificarInactividad(false);
    }

    // Escaneo diario, en horario de baja carga. Para no repetir el aviso todos los dias, se manda el dia en que
    // se cumplen los 20 dias sin actividad y, si el donante sigue sin volver, cada 20 dias mas.
    @Scheduled(cron = "${cron.inactividad:0 0 3 * * *}")
    public void verificarInactividadDiaria() {
        try {
            verificarInactividad(true);
        } catch (RuntimeException e) {
            System.err.println("Falló el escaneo diario de inactividad: " + e.getMessage());
        } finally {
            WithSimplePersistenceUnit.dispose(); // el EntityManager es por hilo y este no es de un request
        }
    }

    private void verificarInactividad(boolean soloAlCumplirElPlazo) {
        System.out.println("Iniciando escaneo de inactividad de donantes...");

        List<Donante> donantes = donanteRepository.findAll();
        LocalDate hoy = LocalDate.now();

        for (Donante donante : donantes) {
            // un donante sin donaciones no tiene fecha de ultima donacion y cortaria el escaneo
            if (donante.getDonaciones().isEmpty()) {
                continue;
            }
            long diasSinDonar = ChronoUnit.DAYS.between(donante.getFechaUltimaDonacion(), hoy);
            if (diasSinDonar <= DIAS_INACTIVIDAD) {
                continue;
            }
            if (soloAlCumplirElPlazo && (diasSinDonar - 1) % DIAS_INACTIVIDAD != 0) {
                continue;
            }
            System.out.println("Se detectó inactividad prolongada en Donante ID #" + donante.getId());
            this.notificarAusenciaDonante(donante);
        }
        System.out.println("Escaneo de inactividad finalizado.");
    }

    public void iniciarRuta(InicioRutaRequest request) {
        for (ParadaRequest parada : request.paradas()) {
            for (Long donacionId : parada.donacionIds()) {
                Donacion donacion = donacionRepository.findById(donacionId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No se encontro una donacion con el id: " + donacionId));

                donacion.cambiarEstado(TipoEstadoDonacion.EN_TRASLADO,
                        "La donación se encuentra en camino a su destino.");
                donacionRepository.save(donacion);
            }
        }
    }

    public void emitirEntregaExitosa(EntidadBeneficiaria entidad, List<Donacion> donaciones, String patente,
            LocalDateTime fechaHora) {
        ComprobanteEntrega comprobante = new ComprobanteEntrega(patente, fechaHora);
        eventManager.emitir(new EventoEntregaExitosa(entidad, donaciones, comprobante));
    }

    public void notificarEntregaFallida(Long donacionId, String motivo) {
        Donacion donacion = donacionRepository.findById(donacionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una donacion con el id: " + donacionId));

        donacion.cambiarEstado(TipoEstadoDonacion.ENTREGA_FALLIDA, "Entrega fallida reportada. Motivo: " + motivo);
        donacionRepository.save(donacion);

        eventManager.emitir(new EventoEntregaFallida(donacion, motivo));
    }

    public void emitirRecepcionConfirmada(Donacion donacion) {
        if (donacion != null) {
            eventManager.emitir(new EventoRecepcionConfirmada(
                    donacion,
                    donacion.getDonante(),
                    donacion.getEntidadBeneficiariaAsignada()));
        }
    }
}
