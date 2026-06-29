package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DonacionRepository {
    private List<Donacion> donaciones = new ArrayList<>();

    private Long proximoId = 1L;

    public Donacion save(Donacion donacion) {
        if (donacion.getId() == null) {
            donacion.setId(proximoId++);
            donaciones.add(donacion);
        } else {
            findById(donacion.getId()).ifPresent(donaciones::remove);
            donaciones.add(donacion);
        }
        return donacion;
    }

    public Optional<Donacion> findById(Long id) {
        if (id == null)
            return Optional.empty();
        return donaciones.stream()
                .filter(d -> id.equals(d.getId()))
                .findFirst();
    }

    public List<Donacion> findAll() {
        return new ArrayList<>(donaciones);
    }

    public boolean deleteById(Long id) {
        Optional<Donacion> donacion = findById(id);
        if (donacion.isPresent()) {
            donaciones.remove(donacion.get());
            return true;
        }
        return false;
    }

    public List<Donacion> buscarPorEstado(TipoEstadoDonacion estadoBuscado) {
        if (estadoBuscado == null)
            return new ArrayList<>();
        return donaciones.stream()
                .filter(d -> d.estadoActual() == estadoBuscado)
                .toList();
    }

    public void limpiar() {
        donaciones.clear();
        proximoId = 1L;
    }
}