package ar.edu.utn.frba.ddsi.logistica.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;

import ar.edu.utn.frba.ddsi.logistica.dto.camion.CamionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.CamionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ChoferResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ChoferRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Chofer;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.services.CamionService;

@Service
public class CamionServiceImpl implements CamionService {

    private final CamionRepository camionRepository;

    public CamionServiceImpl(CamionRepository camionRepository) {
        this.camionRepository = camionRepository;
    }

    public List<CamionResponse> obtenerTodos() {
        return camionRepository.findAll().stream()
                .map(this::toCamionResponse)
                .collect(Collectors.toList());
    }

    public CamionResponse obtenerPorId(Long id) {
        Camion camion = camionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro un camion con el id: " + id));

        return this.toCamionResponse(camion);
    }

    public CamionResponse crear(CamionRequest request) {
        Camion camion = toCamion(request);
        camion = camionRepository.save(camion);
        return this.toCamionResponse(camion);
    }

    public CamionResponse actualizar(Long id, CamionRequest request) {
        Camion existente = camionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro un camion con el id: " + id));

        if (request.patente() != null && !request.patente().isBlank()) {
            existente.setPatente(request.patente());
        }
        if (request.capacidadVolumen() != null && request.capacidadVolumen() > 0) {
            existente.setCapacidadVolumen(request.capacidadVolumen());
        }
        if (request.altura() != null && request.altura() > 0) {
            existente.setAltura(request.altura());
        }
        if (request.capacidadCarga() != null && request.capacidadCarga() > 0) {
            existente.setCapacidadCarga(request.capacidadCarga());
        }
        if (request.chofer() != null) {
            existente.setChofer(toChofer(request.chofer()));
        }

        return this.toCamionResponse(camionRepository.save(existente));
    }

    public boolean eliminar(Long id) {
        return camionRepository.deleteById(id);
    }

    private Camion toCamion(CamionRequest request) {
        if (request.patente() == null || request.patente().isBlank()) {
            throw new BusinessException("La patente del camion no puede ser nula ni estar vacia");
        }
        if (request.capacidadVolumen() == null || request.capacidadVolumen() <= 0) {
            throw new BusinessException("La capacidad de volumen debe ser mayor a 0");
        }
        if (request.altura() == null || request.altura() <= 0) {
            throw new BusinessException("La altura debe ser mayor a 0");
        }
        if (request.capacidadCarga() == null || request.capacidadCarga() <= 0) {
            throw new BusinessException("La capacidad de carga debe ser mayor a 0");
        }
        if (request.chofer() == null) {
            throw new BusinessException("El camion debe tener un chofer asignado");
        }

        return new Camion(request.patente(), request.capacidadVolumen(), request.altura(),
                request.capacidadCarga(), toChofer(request.chofer()));
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
            throw new BusinessException("El chofer no puede ser nulo");
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre del chofer no puede ser nulo ni estar vacio");
        }
        if (request.apellido() == null || request.apellido().isBlank()) {
            throw new BusinessException("El apellido del chofer no puede ser nulo ni estar vacio");
        }

        return new Chofer(request.nombre(), request.apellido());
    }

    private ChoferResponse toChoferResponse(Chofer chofer) {
        return new ChoferResponse(
                chofer.getNombre(),
                chofer.getApellido());
    }
}