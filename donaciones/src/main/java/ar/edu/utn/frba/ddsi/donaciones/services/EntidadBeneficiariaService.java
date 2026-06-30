package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;

@Service
public class EntidadBeneficiariaService {

    private final EntidadBeneficiariaRepository entidadBeneficiariaRepository;
    private final DonacionService donacionService;
    private final EventoService eventoService;

    public EntidadBeneficiariaService(EntidadBeneficiariaRepository entidadBeneficiariaRepository,
            DonacionService donacionService, EventoService eventoService) {
        this.entidadBeneficiariaRepository = entidadBeneficiariaRepository;
        this.donacionService = donacionService;
        this.eventoService = eventoService;
    }

    public List<EntidadBeneficiaria> obtenerTodas() {
        return entidadBeneficiariaRepository.findAll();
    }

    public EntidadBeneficiaria obtenerPorId(Long id) {
        return entidadBeneficiariaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    public EntidadBeneficiaria crear(EntidadBeneficiaria entidad) {
        entidad.setId(null);
        return entidadBeneficiariaRepository.save(entidad);
    }

    public boolean eliminar(Long id) {
        return entidadBeneficiariaRepository.deleteById(id);
    }

    public EntidadBeneficiaria actualizar(Long id, EntidadBeneficiaria entidadActualizada) {
        return entidadBeneficiariaRepository.findById(id)
                .map(entidadExistente -> {
                    entidadExistente.setRazonSocial(entidadActualizada.getRazonSocial());
                    entidadExistente.setDireccion(entidadActualizada.getDireccion());
                    entidadExistente.setTelefono(entidadActualizada.getTelefono());
                    entidadExistente.setCorreoRepresentantes(entidadActualizada.getCorreoRepresentantes());
                    return entidadBeneficiariaRepository.save(entidadExistente);
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    public List<Necesidad> obtenerNecesidades(Long id) {
        return entidadBeneficiariaRepository.findById(id)
                .map(EntidadBeneficiaria::getNecesidades)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    public Necesidad registrarNecesidad(Long id, Necesidad necesidad) {
        return entidadBeneficiariaRepository.findById(id)
                .map(entidad -> {
                    entidad.registrarNecesidad(necesidad);
                    entidadBeneficiariaRepository.save(entidad);
                    return necesidad;
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    public void eliminarNecesidad(Long entidadId, Long necesidadId) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    entidad.eliminarNecesidad(necesidadId);
                    entidadBeneficiariaRepository.save(entidad);
                    return entidad;
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    public void confirmarEntrega(Long entidadId, Long donacionId) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    Donacion donacion = donacionService.obtenerPorId(donacionId);
                    entidad.confirmarEntrega(donacion);
                    entidadBeneficiariaRepository.save(entidad);
                    return entidad;
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    public void reportarNoRecibida(Long entidadId, Long donacionId, String motivo) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    eventoService.notificarEntregaFallida(donacionId, motivo);
                    return entidad;
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));

    }

    public void subirFotosRecepcion(Long entidadId, Long donacionId, List<String> fotosUrl) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    Donacion donacion = donacionService.obtenerPorId(donacionId);
                    if (donacion.getFotosRecepcion() == null) {
                        donacion.setFotosRecepcion(new java.util.ArrayList<>());
                    }
                    donacion.getFotosRecepcion().addAll(fotosUrl);
                    donacionService.guardar(donacion);
                    return entidad;
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

}
