package ar.edu.utn.frba.ddsi.logistica.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.logistica.dto.ruta.ObtenerRutaResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.ObtenerTodasRutasResponse;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;

@Service
public class RutaService {

    private final RutaRepository rutaRepository;

    public RutaService(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    public ObtenerTodasRutasResponse obtenerTodas(LocalDate fecha) {
        List<Ruta> rutas = rutaRepository.findAll();
        if (fecha != null) {
            rutas = rutas.stream()
                    .filter(r -> fecha.equals(r.getFecha()))
                    .collect(Collectors.toList());
        }
        List<ObtenerRutaResponse> responses = rutas.stream()
                .map(r -> new ObtenerRutaResponse(
                        r.getId(), r.getFecha(), r.getEstado(),
                        r.getCamion() != null ? r.getCamion().getPatente() : null
                ))
                .collect(Collectors.toList());
        return new ObtenerTodasRutasResponse(responses);
    }

    public ObtenerRutaResponse obtenerPorId(Long id) {
        Ruta r = rutaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la ruta con id: " + id));
        return new ObtenerRutaResponse(
                r.getId(), r.getFecha(), r.getEstado(),
                r.getCamion() != null ? r.getCamion().getPatente() : null
        );
    }

    public boolean eliminar(Long id) {
        return rutaRepository.deleteById(id);
    }
}
