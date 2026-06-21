package ar.edu.utn.frba.ddsi.logistica.models.repositories;

import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CamionRepository {
    private List<Camion> camiones = new ArrayList<>();

    private Long proximoId = 1L;

    /**
     * Guarda o actualiza un camión en la flota de la organización.
     */
    public Camion save(Camion camion) {
        if (camion.getId() == null) {
            camion.setId(proximoId++);
            camiones.add(camion);
        } else {
            findById(camion.getId()).ifPresent(camiones::remove);
            camiones.add(camion);
        }
        return camion;
    }

    /**
     * Busca un camión específico por su ID único (Para GET /api/camiones/{id}).
     */
    public Optional<Camion> findById(Long id) {
        if (id == null)
            return Optional.empty();
        return camiones.stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst();
    }

    /**
     * Busca un camión por su patente (Útil para validaciones de negocio).
     */
    public Optional<Camion> findByPatente(String patente) {
        if (patente == null)
            return Optional.empty();
        return camiones.stream()
                .filter(c -> patente.equalsIgnoreCase(c.getPatente()))
                .findFirst();
    }

    /**
     * Retorna toda la flota registrada (Esencial para GET /api/camiones y el
     * Planificador).
     */
    public List<Camion> findAll() {
        return new ArrayList<>(camiones);
    }

    /**
     * Elimina un camión de la flota (Para DELETE /api/camiones/{id}).
     */
    public boolean deleteById(Long id) {
        Optional<Camion> camion = findById(id);
        if (camion.isPresent()) {
            camiones.remove(camion.get());
            return true;
        }
        return false;
    }

    public void limpiar() {
        camiones.clear();
        proximoId = 1L;
    }
}