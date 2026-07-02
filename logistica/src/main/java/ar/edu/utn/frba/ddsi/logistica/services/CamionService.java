package ar.edu.utn.frba.ddsi.logistica.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.logistica.dto.camion.CamionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.CamionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ChoferResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ChoferRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Chofer;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;

@Service
public class CamionService {

    private final CamionRepository camionRepository;

    public CamionService(CamionRepository camionRepository) {
        this.camionRepository = camionRepository;
    }

    public List<CamionResponse> obtenerTodos() {
        return camionRepository.findAll().stream()
                .map(this::toCamionResponse)
                .collect(Collectors.toList());
    }

    public CamionResponse obtenerPorId(Long id) {
        Camion camion = camionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el camion"));

        return this.toCamionResponse(camion);
    }

    public CamionResponse crear(CamionRequest request) {
        Camion camion = toCamion(request);
        camion = camionRepository.save(camion);
        return this.toCamionResponse(camion);
    }

    public CamionResponse actualizar(Long id, CamionRequest request) {
        Camion existente = camionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el camion"));

        existente.setPatente(request.patente());
        existente.setCapacidadVolumen(request.capacidadVolumen());
        existente.setAltura(request.altura());
        existente.setCapacidadCarga(request.capacidadCarga());
        existente.setChofer(toChofer(request.chofer()));

        existente = camionRepository.save(existente);
        return this.toCamionResponse(existente);
    }

    public boolean eliminar(Long id) {
        return camionRepository.deleteById(id);
    }

    private Camion toCamion(CamionRequest request) {
        Camion camion = new Camion();
        camion.setPatente(request.patente());
        camion.setCapacidadVolumen(request.capacidadVolumen());
        camion.setAltura(request.altura());
        camion.setCapacidadCarga(request.capacidadCarga());
        camion.setChofer(toChofer(request.chofer()));
        return camion;
    }

    private CamionResponse toCamionResponse(Camion camion) {
        return new CamionResponse(
                camion.getId(),
                camion.getPatente(),
                camion.getCapacidadVolumen(),
                camion.getAltura(),
                camion.getCapacidadCarga(),
                toChoferResponse(camion.getChofer()));
    }

    private Chofer toChofer(ChoferRequest request) {
        if (request == null)
            return null;
        Chofer chofer = new Chofer();
        chofer.setNombre(request.nombre());
        chofer.setApellido(request.apellido());
        return chofer;
    }

    private ChoferResponse toChoferResponse(Chofer chofer) {
        return new ChoferResponse(
                chofer.getNombre(),
                chofer.getApellido());
    }
}