package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.Map;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEvento;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.notifiaciones.EventManager;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.notifiaciones.Evento;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;

@Service
public class NotificacionEventoService {

    private final EventManager eventManager;
    private final DonacionRepository donacionRepository;
    private final EntidadBeneficiariaRepository entidadBeneficiariaRepository;

    public NotificacionEventoService(EventManager eventManager, DonacionRepository donacionRepository,
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

    public void notificarInicioRutaDonante(Long idDonacion) {
        Donacion donacion = donacionRepository.findById(idDonacion)
                .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada."));

        Donante donante = donacion.getDonante();
        Map<String, Object> datos = Map.of("contacto", donante.getContactoPredeterminado());
        Evento eventoInicioRuta = new Evento(TipoEvento.INICIO_RUTA_DONANTE, datos);
        eventManager.emitir(eventoInicioRuta);
    }

    public void notificarInicioRutaEntidad(Long idEntidad) {
        EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(idEntidad)
                .orElseThrow(() -> new IllegalArgumentException("EntidadBeneficiaria no encontrada."));

        for (MedioContacto contacto : entidad.getCorreoRepresentantes()) {
            Map<String, Object> datos2 = Map.of("contacto", contacto);
            Evento eventoInicioRuta2 = new Evento(TipoEvento.INICIO_RUTA_ENTIDAD, datos2);
            eventManager.emitir(eventoInicioRuta2);
        }

        Map<String, Object> datos3 = Map.of("contacto", entidad.getTelefono());
        Evento eventoInicioRuta3 = new Evento(TipoEvento.INICIO_RUTA_ENTIDAD, datos3);
        eventManager.emitir(eventoInicioRuta3);
    }

    public void notificarConfirmacionEntregaExitosa(Long idDonacion) {
        Donacion donacion = donacionRepository.findById(idDonacion)
                .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada."));

        Donante donante = donacion.getDonante();
        Map<String, Object> datos = Map.of("contacto", donante.getContactoPredeterminado());
        Evento eventoConfirmacionEntregaExitosa = new Evento(TipoEvento.ENTREGA_EXITOSA_DONANTE, datos);
        eventManager.emitir(eventoConfirmacionEntregaExitosa);

        EntidadBeneficiaria entidad = donacion.getEntidadBeneficiariaAsignada();

        for (MedioContacto contacto : entidad.getCorreoRepresentantes()) {
            Map<String, Object> datos2 = Map.of("contacto", contacto);
            Evento eventoInicioRuta2 = new Evento(TipoEvento.ENTREGA_EXITOSA_ENTIDAD, datos2);
            eventManager.emitir(eventoInicioRuta2);
        }

        Map<String, Object> datos3 = Map.of("contacto", entidad.getTelefono());
        Evento eventoInicioRuta3 = new Evento(TipoEvento.ENTREGA_EXITOSA_ENTIDAD, datos3);
        eventManager.emitir(eventoInicioRuta3);
    }
}
