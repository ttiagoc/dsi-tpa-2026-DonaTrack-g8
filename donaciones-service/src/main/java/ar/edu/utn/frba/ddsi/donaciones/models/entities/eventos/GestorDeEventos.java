package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

@Component
public class GestorDeEventos {

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

    public void verificarInactividadDonantes() {
        System.out.println("Iniciando escaneo diario de inactividad de donantes...");

        List<Donante> donantes = donanteRepository.findAll();
        LocalDate limiteInactividad = LocalDate.now().minusDays(20);

        for (Donante donante : donantes) {
            if (donante.getFechaUltimaDonacion().isBefore(limiteInactividad)) {
                System.out.println("Se detectó inactividad prolongada en Donante ID #" + donante.getId());
                this.notificarAusenciaDonante(donante);
            }
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
}
