package ar.edu.utn.frba.ddsi.donaciones.services.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.InicioRutaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ParadaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.ComprobanteEntrega;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventManager;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.Evento;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoAusenciaPlataforma;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoEntregaExitosaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoEntregaExitosaEntidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoEntregaFallida;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoInicioRutaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoInicioRutaEntidad;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.EventoService;

@Service
public class EventoServiceImpl implements EventoService {

    private final EventManager eventManager;
    private final DonacionRepository donacionRepository;
    private final EntidadBeneficiariaRepository entidadBeneficiariaRepository;
    private final DonanteRepository donanteRepository;

    public EventoServiceImpl(EventManager eventManager, DonacionRepository donacionRepository,
            EntidadBeneficiariaRepository entidadBeneficiariaRepository, DonanteRepository donanteRepository) {
        this.eventManager = eventManager;
        this.donacionRepository = donacionRepository;
        this.entidadBeneficiariaRepository = entidadBeneficiariaRepository;
        this.donanteRepository = donanteRepository;
    }

    public void notificarAusenciaDonante(Donante donante) {
        Evento eventoAusencia = new EventoAusenciaPlataforma(donante.getContactoPredeterminado());
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

            EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(parada.entidadId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontro una entidad beneficiaria con el id: " + parada.entidadId()));

            for (MedioContacto contacto : entidad.getCorreoRepresentantes()) {
                Evento evento = new EventoInicioRutaEntidad(contacto,
                        "http://localhost:8080/api/logistica-service/monitoreo/ubicacion/" + request.rutaId());
                eventManager.emitir(evento);
            }

            for (Long donacionId : parada.donacionIds()) {
                Donacion donacion = donacionRepository.findById(donacionId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No se encontro una donacion con el id: " + donacionId));

                donacion.cambiarEstado(TipoEstadoDonacion.EN_TRASLADO,
                        "La donación se encuentra en camino a su destino.");
                donacionRepository.save(donacion);

                Donante donante = donacion.getDonante();
                Evento evento = new EventoInicioRutaDonante(donante.getContactoPredeterminado(),
                        "http://localhost:8080/api/logistica-service/monitoreo/ubicacion/" + request.rutaId());
                eventManager.emitir(evento);
            }
        }
    }

    public void confirmarEntregaExitosa(ConfirmacionEntregaExitosaRequest request) {
        EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(request.entidadId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una entidad beneficiaria con el id: " + request.entidadId()));

        ComprobanteEntrega comprobante = new ComprobanteEntrega(request.patenteCamion(), request.fechaHora());

        for (Long donacionId : request.donacionIds()) {
            Donacion donacion = donacionRepository.findById(donacionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontro una donacion con el id: " + donacionId));

            entidad.confirmarEntrega(donacion);
            donacionRepository.save(donacion);

            Donante donante = donacion.getDonante();
            Evento evDonante = new EventoEntregaExitosaDonante(
                    donante.getContactoPredeterminado(), comprobante);
            eventManager.emitir(evDonante);
        }

        for (MedioContacto correo : entidad.getCorreoRepresentantes()) {
            Evento evento = new EventoEntregaExitosaEntidad(correo, comprobante);
            eventManager.emitir(evento);
        }
    }

    public void notificarEntregaFallida(Long donacionId, String motivo) {
        Donacion donacion = donacionRepository.findById(donacionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una donacion con el id: " + donacionId));

        donacion.cambiarEstado(TipoEstadoDonacion.ENTREGA_FALLIDA, "Entrega fallida reportada. Motivo: " + motivo);
        donacionRepository.save(donacion);

        EntidadBeneficiaria entidad = donacion.getEntidadBeneficiariaAsignada();
        Donante donante = donacion.getDonante();

        Evento eventoDonante = new EventoEntregaFallida(
                donante.getContactoPredeterminado(), donacionId, motivo);
        eventManager.emitir(eventoDonante);

        for (MedioContacto correo : entidad.getCorreoRepresentantes()) {
            Evento eventoEntidad = new EventoEntregaFallida(
                    correo, donacionId, motivo);
            eventManager.emitir(eventoEntidad);
        }
    }
}
