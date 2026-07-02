package ar.edu.utn.frba.ddsi.logistica.models.repositories.impl;

import ar.edu.utn.frba.ddsi.common.utils.GeneradorIdSecuencial;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;

@Repository
public class InMemoryCamion implements CamionRepository {

    private final RutaRepository rutaRepository;

    public InMemoryCamion(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    private List<Camion> camiones = new ArrayList<>();

    private GeneradorIdSecuencial generadorId = new GeneradorIdSecuencial();

    public Camion save(Camion camion) {
        if (camion.getId() == null) {
            camion.setId(generadorId.siguiente());
            camiones.add(camion);
        } else {
            findById(camion.getId()).ifPresent(camiones::remove);
            camiones.add(camion);
        }
        return camion;
    }

    public Optional<Camion> findById(Long id) {
        if (id == null)
            return Optional.empty();
        return camiones.stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst();
    }

    public Optional<Camion> findByPatente(String patente) {
        if (patente == null)
            return Optional.empty();
        return camiones.stream()
                .filter(c -> patente.equalsIgnoreCase(c.getPatente()))
                .findFirst();
    }

    public List<Camion> findAll() {
        return new ArrayList<>(camiones);
    }

    public List<Camion> findAllDisponibles() {
        return camiones.stream()
                .filter(c -> estaDisponible(c))
                .collect(Collectors.toList());
    }

    private Boolean estaDisponible(Camion camion) {
        List<Ruta> rutas = rutaRepository.buscarRutasActivasPorCamion(camion.getId());
        return rutas.isEmpty();
    }

    public boolean deleteById(Long id) {
        Optional<Camion> camion = findById(id);
        if (camion.isPresent()) {
            camiones.remove(camion.get());
            return true;
        }
        return false;
    }
}