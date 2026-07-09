package ar.edu.utn.frba.ddsi.donaciones.services.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.InicioRutaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ParadaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.ComprobanteEntrega;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventManagerDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoAusenciaPlataforma;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoEntregaExitosa;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoEntregaFallida;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.EventoService;

@Service
public class EventoServiceImpl implements EventoService {

    private final EventManagerDonaciones eventManager;
    private final DonacionRepository donacionRepository;
    private final EntidadBeneficiariaRepository entidadBeneficiariaRepository;
    private final DonanteRepository donanteRepository;

    public EventoServiceImpl(EventManagerDonaciones eventManager, DonacionRepository donacionRepository,
            EntidadBeneficiariaRepository entidadBeneficiariaRepository, DonanteRepository donanteRepository) {
        this.eventManager = eventManager;
        this.donacionRepository = donacionRepository;
        this.entidadBeneficiariaRepository = entidadBeneficiariaRepository;
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

    public void confirmarEntregaExitosa(ConfirmacionEntregaExitosaRequest request) {
        EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(request.entidadId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una entidad beneficiaria con el id: " + request.entidadId()));

        ComprobanteEntrega comprobante = new ComprobanteEntrega(request.patenteCamion(), request.fechaHora());
        List<Donacion> donaciones = new java.util.ArrayList<>();

        for (Long donacionId : request.donacionIds()) {
            Donacion donacion = donacionRepository.findById(donacionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontro una donacion con el id: " + donacionId));

            entidad.confirmarEntrega(donacion);
            donacionRepository.save(donacion);
            donaciones.add(donacion);
        }

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
