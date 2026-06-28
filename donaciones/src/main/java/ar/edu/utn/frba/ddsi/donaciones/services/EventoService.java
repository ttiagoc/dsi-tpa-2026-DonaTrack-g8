package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.Map;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEvento;
import ar.edu.utn.frba.ddsi.donaciones.dto.EntregaExitosaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.InicioRutaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.ParadaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.notifiaciones.EventManager;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.notifiaciones.Evento;
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
        Map<String, Object> datos = Map.of("contacto", donante.getContactoPredeterminado());
        Evento eventoAusencia = new Evento(TipoEvento.AUSENCIA_PLATAFORMA, datos);
        eventManager.emitir(eventoAusencia);
    }

    public void iniciarRuta(InicioRutaDTO dto) {
        for (ParadaDTO parada : dto.getParadas()) {

            EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(parada.getEntidadId())
                    .orElseThrow(() -> new IllegalArgumentException("Entidad no encontrada."));

            for (MedioContacto contacto : entidad.getCorreoRepresentantes()) {
                Evento evento = new Evento(TipoEvento.INICIO_RUTA_ENTIDAD,
                        Map.of("contacto", contacto));
                eventManager.emitir(evento);
            }

            for (Long donacionId : parada.getDonacionIds()) {
                Donacion donacion = donacionRepository.findById(donacionId)
                        .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada."));

                donacion.cambiarEstado(TipoEstadoDonacion.EN_TRASLADO, "Donación arriba del camión en viaje.");
                donacionRepository.save(donacion);

                Donante donante = donacion.getDonante();
                Evento evento = new Evento(TipoEvento.INICIO_RUTA_DONANTE,
                        Map.of("contacto", donante.getContactoPredeterminado()));
                eventManager.emitir(evento);
            }
        }
    }

    public void confirmarEntregaExitosa(EntregaExitosaDTO dto) {
        EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(dto.getEntidadId())
                .orElseThrow(() -> new IllegalArgumentException("Entidad Beneficiaria no encontrada."));

        Map<String, Object> comprobante = Map.of(
                "patenteCamion", dto.getPatenteCamion(),
                "fechaHora", dto.getFechaHora());

        for (Long donacionId : dto.getDonacionIds()) {
            Donacion donacion = donacionRepository.findById(donacionId).orElse(null);

            if (donacion != null) {
                donacion.cambiarEstado(TipoEstadoDonacion.ENTREGADA,
                        "Entrega confirmada en sede por la entidad.");
                donacionRepository.save(donacion);

                entidad.confirmarEntrega(donacion);

                Donante donante = donacion.getDonante();
                if (donante != null) {
                    Evento evDonante = new Evento(TipoEvento.ENTREGA_EXITOSA_DONANTE, Map.of(
                            "contacto", donante.getContactoPredeterminado(),
                            "comprobante", comprobante));
                    eventManager.emitir(evDonante);
                }
            }
        }

        for (MedioContacto correo : entidad.getCorreoRepresentantes()) {
            Evento evento = new Evento(TipoEvento.ENTREGA_EXITOSA_ENTIDAD,
                    Map.of("contacto", correo, "comprobante", comprobante));
            eventManager.emitir(evento);
        }
    }
}
