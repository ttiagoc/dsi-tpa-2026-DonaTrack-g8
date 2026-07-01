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

    public EntidadBeneficiaria save(EntidadBeneficiaria entidad) {
        if (entidad.getId() == null) {
            entidad.setId(proximoId++);
            entidades.add(entidad);
        } else {
            findById(entidad.getId()).ifPresent(entidades::remove);
            entidades.add(entidad);
        }
        return entidad;
    }

    public Optional<EntidadBeneficiaria> findById(Long id) {
        if (id == null)
            return Optional.empty();
        return entidades.stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst();
    }

    public List<EntidadBeneficiaria> findAll() {
        return new ArrayList<>(entidades);
    }

    public boolean deleteById(Long id) {
        Optional<EntidadBeneficiaria> entidad = findById(id);
        if (entidad.isPresent()) {
            entidades.remove(entidad.get());
            return true;
        }
        return false;
    }

    public void limpiar() {
        entidades.clear();
        proximoId = 1L;
    }

    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }
}