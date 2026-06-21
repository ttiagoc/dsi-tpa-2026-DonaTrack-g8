package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class EntidadBeneficiariaRepository {
    private List<EntidadBeneficiaria> entidades = new ArrayList<>();

    private Long proximoId = 1L;

    /**
     * Guarda o actualiza una entidad beneficiaria en el sistema.
     */
    public EntidadBeneficiaria save(EntidadBeneficiaria entidad) {
        if (entidad.getId() == null) {
            // Caso CREATE: Asignamos ID nuevo y guardamos
            entidad.setId(proximoId++);
            entidades.add(entidad);
        } else {
            // Caso UPDATE: Reemplazamos la versión vieja por la actualizada
            findById(entidad.getId()).ifPresent(entidades::remove);
            entidades.add(entidad);
        }
        return entidad;
    }

    /**
     * Busca una entidad beneficiaria por su ID único (Esencial para GET
     * /api/entidades/{id}).
     */
    public Optional<EntidadBeneficiaria> findById(Long id) {
        if (id == null)
            return Optional.empty();
        return entidades.stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst();
    }

    /**
     * Retorna todas las entidades registradas (Esencial para GET /api/entidades).
     */
    public List<EntidadBeneficiaria> findAll() {
        return new ArrayList<>(entidades);
    }

    /**
     * Elimina una entidad beneficiaria por su ID (Para DELETE /api/entidades/{id}).
     */
    public boolean deleteById(Long id) {
        Optional<EntidadBeneficiaria> entidad = findById(id);
        if (entidad.isPresent()) {
            entidades.remove(entidad.get());
            return true;
        }
        return false;
    }

    /**
     * Limpia la "base de datos" en memoria antes de los tests.
     */
    public void limpiar() {
        entidades.clear();
        proximoId = 1L;
    }
}