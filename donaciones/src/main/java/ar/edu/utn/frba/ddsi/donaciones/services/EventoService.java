package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.Map;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.dto.EntregaExitosaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.InicioRutaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.ParadaDTO;
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
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;

@Service
public class EventoService {

    private final EventManager eventManager;
    private final DonacionRepository donacionRepository;
    private final EntidadBeneficiariaRepository entidadBeneficiariaRepository;

    public EventoService(EventManager eventManager, DonacionRepository donacionRepository,
            EntidadBeneficiariaRepository entidadBeneficiariaRepository) {
        this.eventManager = eventManager;
        this.donacionRepository = donacionRepository;
        this.entidadBeneficiariaRepository = entidadBeneficiariaRepository;
    }

    public void notificarAusenciaDonante(Donante donante) {
        Evento eventoAusencia = new EventoAusenciaPlataforma(donante.getContactoPredeterminado());
        eventManager.emitir(eventoAusencia);
    }

    public void iniciarRuta(InicioRutaDTO dto) {
        for (ParadaDTO parada : dto.getParadas()) {

            EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(parada.getEntidadId())
                    .orElseThrow(() -> new IllegalArgumentException("Entidad no encontrada."));

            for (MedioContacto contacto : entidad.getCorreoRepresentantes()) {
                Evento evento = new EventoInicioRutaDonante(contacto, "URL_MAPA"); // TODO: Falta el mapa
                eventManager.emitir(evento);
            }

            for (Long donacionId : parada.getDonacionIds()) {
                Donacion donacion = donacionRepository.findById(donacionId)
                        .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada."));

                donacion.cambiarEstado(TipoEstadoDonacion.EN_TRASLADO, "Donación arriba del camión en viaje.");
                donacionRepository.save(donacion);

                Donante donante = donacion.getDonante();
                Evento evento = new EventoInicioRutaDonante(donante.getContactoPredeterminado(), "URL_MAPA"); // TODO:
                                                                                                              // Falta
                                                                                                              // el mapa
                eventManager.emitir(evento);
            }
        }
    }

    public void confirmarEntregaExitosa(EntregaExitosaDTO dto) {
        EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(dto.getEntidadId())
                .orElseThrow(() -> new IllegalArgumentException("Entidad Beneficiaria no encontrada."));

        ComprobanteEntrega comprobante = new ComprobanteEntrega(dto.getPatenteCamion(), dto.getFechaHora());

        for (Long donacionId : dto.getDonacionIds()) {
            Donacion donacion = donacionRepository.findById(donacionId).orElse(null);

            if (donacion != null) {
                donacion.cambiarEstado(TipoEstadoDonacion.ENTREGADA,
                        "Entrega confirmada en sede por la entidad.");
                donacionRepository.save(donacion);

                entidad.confirmarEntrega(donacion);

                Donante donante = donacion.getDonante();
                if (donante != null) {
                    Evento evDonante = new EventoEntregaExitosaDonante(
                            donante.getContactoPredeterminado(), comprobante);
                    eventManager.emitir(evDonante);
                }
            }
        }

        for (MedioContacto correo : entidad.getCorreoRepresentantes()) {
            Evento evento = new EventoEntregaExitosaEntidad(correo, comprobante);
            eventManager.emitir(evento);
        }
    }

    public void notificarEntregaFallida(Long donacionId, String motivo) {
        Donacion donacion = donacionRepository.findById(donacionId)
                .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada."));

        donacion.cambiarEstado(TipoEstadoDonacion.ENTREGA_FALLIDA, "Entrega fallida reportada. Motivo: " + motivo);
        donacionRepository.save(donacion);

        EntidadBeneficiaria entidad = donacion.getEntidadBeneficiariaAsignada();
        Donante donante = donacion.getDonante();

        Map<String, Object> datosComunes = Map.of(
                "donacionId", donacionId,
                "motivo", motivo);

        if (donante != null && donante.getContactoPredeterminado() != null) {
            Evento eventoDonante = new EventoEntregaFallida(
                    donante.getContactoPredeterminado()); // TODO: Faltan los datosComunes
            eventManager.emitir(eventoDonante);
        }

        if (entidad != null) {
            for (MedioContacto correo : entidad.getCorreoRepresentantes()) {
                Evento eventoEntidad = new EventoEntregaFallida(
                        correo); // TODO: Faltan los datosComunes
                eventManager.emitir(eventoEntidad);
            }
        }
    }
}
