package ar.edu.utn.frba.ddsi.logistica.services;

import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;

@Service
public class CamionService {

    private final CamionRepository camionRepository;

    public CamionService(CamionRepository camionRepository) {
        this.camionRepository = camionRepository;
    }

    public List<Camion> obtenerTodos() {
        return camionRepository.findAll();
    }

    public Camion obtenerPorId(Long id) {
        return camionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el camion con id: " + id));
    }

    public Camion crear(Camion camion) {
        camion.setId(null);
        return camionRepository.save(camion);
    }

    public Camion actualizar(Long id, Camion camionActualizado) {
        Camion existente = camionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el camion con id: " + id));

        existente.setPatente(camionActualizado.getPatente());
        existente.setCapacidadVolumen(camionActualizado.getCapacidadVolumen());
        existente.setAltura(camionActualizado.getAltura());
        existente.setCapacidadCarga(camionActualizado.getCapacidadCarga());
        existente.setChofer(camionActualizado.getChofer());

        return camionRepository.save(existente);
    }

    public boolean eliminar(Long id) {
        return camionRepository.deleteById(id);
    }
}
