package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.SegmentadorDeDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;

@Service
public class DonacionService {

    private final DonacionRepository donacionRepository;
    private final SegmentadorDeDonacion segmentadorDeDonacion;

    public DonacionService(DonacionRepository donacionRepository, SegmentadorDeDonacion segmentadorDeDonacion) {
        this.donacionRepository = donacionRepository;
        this.segmentadorDeDonacion = segmentadorDeDonacion;
    }

    public List<Donacion> obtenerTodas() {
        return donacionRepository.findAll();
    }

    public Donacion obtenerPorId(Long id) {
        return donacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
    }

    public List<Donacion> crear(RegistroDonacion donacion) {
        List<Donacion> donacionesCreadas = segmentadorDeDonacion.segmentarDonacion(donacion);
        donacionesCreadas.forEach(d -> d.setId(null));
        return donacionRepository.saveAll(donacionesCreadas);
    }

    public boolean eliminar(Long id) {
        return donacionRepository.deleteById(id);
    }

    public void cambiarEstado(Long id, TipoEstadoDonacion nuevoEstado, String justificacion) {
        Donacion donacion = donacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
        donacion.cambiarEstado(nuevoEstado, justificacion);
        donacionRepository.save(donacion);
    }

    public List<DonacionDTO> obtenerDonacionesAsignadas(int limit) {
        List<Donacion> donaciones = donacionRepository.buscarPorEstado(TipoEstadoDonacion.ASIGNACION_REALIZADA)
                .subList(0, limit);

        List<DonacionDTO> donacionesAsignadas = new ArrayList<>();
        for (Donacion donacion : donaciones) {
            donacionesAsignadas.add(toDto(donacion));
        }

        return donacionesAsignadas;
    }

    private DonacionDTO toDto(Donacion donacion) {
        return new DonacionDTO(
                donacion.getId(),
                donacion.calcularPesoTotal(),
                donacion.calcularVolumenTotal(),
                donacion.obtenerDireccion());
    }

    public void donacionesEntregaLista(List<DonacionDTO> donaciones) {
        for (DonacionDTO dto : donaciones) {
            Donacion donacion = Optional.ofNullable(donacionRepository.findById(dto.getId()))
                    .map(Optional::get)
                    .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
            donacion.cambiarEstado(TipoEstadoDonacion.LISTA_PARA_ENTREGAR, null);
        }
    }

    public Donacion replanificar(Long id) {
        Donacion donacion = donacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
        donacion.cambiarEstado(TipoEstadoDonacion.EN_DEPOSITO, "Replanificación de entrega fallida");
        return donacionRepository.save(donacion);
    }

    public Donacion guardar(Donacion donacion) {
        return donacionRepository.save(donacion);
    }

}
