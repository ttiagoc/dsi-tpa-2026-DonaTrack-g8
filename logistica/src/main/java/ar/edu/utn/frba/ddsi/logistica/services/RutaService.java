package ar.edu.utn.frba.ddsi.logistica.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.logistica.dto.ruta.ParadaResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaResponse;
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

    // --- MÉTODOS DE MAPEO ---

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
            paradas
        );
    }

    private ParadaResponse toParadaResponse(Parada parada) {
        return new ParadaResponse(
            parada.getOrden(),
            parada.getDestino(),
            parada.getEntidad(),
            parada.getEntregas()
        );
    }
}