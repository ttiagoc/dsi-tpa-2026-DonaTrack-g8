package ar.edu.utn.frba.ddsi.logistica.services.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.ParadaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.ParadaResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaResponse;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;
import ar.edu.utn.frba.ddsi.logistica.services.RutaService;

@Service
public class RutaServiceImpl implements RutaService {

    private final RutaRepository rutaRepository;
    private final CamionRepository camionRepository;

    public RutaServiceImpl(RutaRepository rutaRepository, CamionRepository camionRepository) {
        this.rutaRepository = rutaRepository;
        this.camionRepository = camionRepository;
    }

    public List<RutaResponse> obtenerTodas(LocalDate fecha) {
        List<Ruta> rutas = rutaRepository.findAll();
        if (fecha != null) {
            rutas = rutas.stream()
                    .filter(r -> fecha.equals(r.getFecha()))
                    .collect(Collectors.toList());
        }

        return rutas.stream()
                .map(this::toRutaResponse)
                .collect(Collectors.toList());
    }

    public RutaResponse obtenerPorId(Long id) {
        Ruta r = rutaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + id));

        return this.toRutaResponse(r);
    }

    public boolean eliminar(Long id) {
        return rutaRepository.deleteById(id);
    }

    public RutaResponse crear(RutaRequest request) {
        Ruta ruta = toRuta(request);
        ruta = rutaRepository.save(ruta);
        return this.toRutaResponse(ruta);
    }

    public RutaResponse actualizar(Long id, RutaRequest request) {
        Ruta existente = rutaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + id));
        if (request.fecha() != null) {
            existente.setFecha(request.fecha());
        }
        if (request.idCamion() != null) {
            Camion camion = camionRepository.findById(request.idCamion())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontro un camion con el id: " + request.idCamion()));
            existente.setCamion(camion);
        }
        if (request.paradas() != null && !request.paradas().isEmpty()) {
            existente.setParadas(request.paradas().stream().map(this::toParada).collect(Collectors.toList()));
        }

        existente = rutaRepository.save(existente);
        return this.toRutaResponse(existente);
    }

    private RutaResponse toRutaResponse(Ruta ruta) {
        String patenteCamion = ruta.getCamion() != null ? ruta.getCamion().getPatente() : null;

        List<ParadaResponse> paradas = ruta.getParadas() != null
                ? ruta.getParadas().stream().map(this::toParadaResponse).collect(Collectors.toList())
                : null;

        return new RutaResponse(
                ruta.getId(),
                ruta.getFecha(),
                ruta.getEstado(),
                patenteCamion,
                paradas);
    }

    private ParadaResponse toParadaResponse(Parada parada) {
        return new ParadaResponse(
                parada.getOrden(),
                parada.getDestino(),
                parada.getEntidad(),
                parada.getEntregas());
    }

    private Ruta toRuta(RutaRequest request) {
        if (request.fecha() == null) {
            throw new BusinessException("La fecha de la ruta no puede ser nula");
        }
        if (request.paradas() == null || request.paradas().isEmpty()) {
            throw new BusinessException("La ruta debe tener al menos una parada");
        }

        Camion camion = camionRepository.findById(request.idCamion())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro un camion con el id: " + request.idCamion()));
        List<Parada> paradas = request.paradas().stream().map(this::toParada).collect(Collectors.toList());

        return new Ruta(request.fecha(), camion, paradas);
    }

    private Parada toParada(ParadaRequest request) {
        if (request.orden() == null || request.orden() < 0) {
            throw new BusinessException("El orden de la parada debe ser mayor o igual a 0");
        }
        if (request.destino() == null || request.destino().isBlank()) {
            throw new BusinessException("El destino de la parada no puede ser nulo ni estar vacio");
        }
        if (request.entidad() == null) {
            throw new BusinessException("La entidad de la parada no puede ser nula");
        }
        if (request.entregas() == null || request.entregas().isEmpty()) {
            throw new BusinessException("La parada debe tener al menos una entrega");
        }

        Parada parada = new Parada();
        parada.setOrden(request.orden());
        parada.setDestino(request.destino());
        parada.setEntidad(request.entidad());
        parada.setEntregas(request.entregas());
        return parada;
    }
}