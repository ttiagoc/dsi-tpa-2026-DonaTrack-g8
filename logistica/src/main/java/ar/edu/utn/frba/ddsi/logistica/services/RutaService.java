package ar.edu.utn.frba.ddsi.logistica.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.logistica.dto.ruta.ParadaResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.ParadaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;

@Service
public class RutaService {

    private final RutaRepository rutaRepository;

    public RutaService(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
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
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la ruta con id: " + id));

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
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la ruta con id: " + id));

        existente.setFecha(request.fecha());
        existente.setEstado(request.estado());

        if (request.patenteCamion() != null) {
            Camion camion = new Camion();
            camion.setPatente(request.patenteCamion());
            existente.setCamion(camion);
        } else {
            existente.setCamion(null);
        }

        if (request.paradas() != null) {
            existente.setParadas(request.paradas().stream().map(this::toParada).collect(Collectors.toList()));
        } else {
            existente.setParadas(null);
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
        Ruta ruta = new Ruta();
        ruta.setFecha(request.fecha());
        ruta.setEstado(request.estado());

        if (request.patenteCamion() != null) {
            Camion camion = new Camion();
            camion.setPatente(request.patenteCamion());
            ruta.setCamion(camion);
        }

        if (request.paradas() != null) {
            ruta.setParadas(request.paradas().stream().map(this::toParada).collect(Collectors.toList()));
        }

        return ruta;
    }

    private Parada toParada(ParadaRequest request) {
        Parada parada = new Parada();
        parada.setOrden(request.orden());
        parada.setDestino(request.destino());
        parada.setEntidad(request.entidad());
        parada.setEntregas(request.entregas());
        return parada;
    }
}