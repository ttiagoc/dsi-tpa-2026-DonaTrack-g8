package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.TipoEstadoDonacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DonacionRepository {
    private List<Donacion> donaciones = new ArrayList<>();

    private Long proximoId = 1L;

    /**
     * Guarda o actualiza una donación en el sistema.
     */
    public Donacion save(Donacion donacion) {
        if (donacion.getId() == null) {
            // Caso CREATE: Asignamos ID nuevo y guardamos
            donacion.setId(proximoId++);
            donaciones.add(donacion);
        } else {
            // Caso UPDATE: Reemplazamos la versión vieja por la actualizada
            findById(donacion.getId()).ifPresent(donaciones::remove);
            donaciones.add(donacion);
        }
        return donacion;
    }

    /**
     * Busca una donación específica por su ID único.
     */
    public Optional<Donacion> findById(Long id) {
        if (id == null)
            return Optional.empty();
        return donaciones.stream()
                .filter(d -> id.equals(d.getId()))
                .findFirst();
    }

    /**
     * Retorna todas las donaciones registradas (Esencial para GET /api/donaciones).
     */
    public List<Donacion> findAll() {
        return new ArrayList<>(donaciones);
    }

    /**
     * Elimina una donación por su ID (Para DELETE /api/donaciones/{id}).
     */
    public boolean deleteById(Long id) {
        Optional<Donacion> donacion = findById(id);
        if (donacion.isPresent()) {
            donaciones.remove(donacion.get());
            return true;
        }
        return false;
    }

    /**
     * Filtra las donaciones por su estado actual.
     */
    public List<Donacion> buscarPorEstado(TipoEstadoDonacion estadoBuscado) {
        if (estadoBuscado == null)
            return new ArrayList<>();
        return donaciones.stream()
                .filter(d -> d.estadoActual() == estadoBuscado)
                .toList();
    }

    /**
     * Limpia la "base de datos" (Ideal para ejecutar antes de cada Test).
     */
    public void limpiar() {
        donaciones.clear();
        proximoId = 1L;
    }
}