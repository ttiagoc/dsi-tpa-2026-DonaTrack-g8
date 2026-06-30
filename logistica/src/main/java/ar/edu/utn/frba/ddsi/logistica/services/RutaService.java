package ar.edu.utn.frba.ddsi.logistica.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;

@Service
public class RutaService {

    private final RutaRepository rutaRepository;

    public RutaService(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    public List<Ruta> obtenerTodas(LocalDate fecha) {
        List<Ruta> rutas = rutaRepository.findAll();
        if (fecha != null) {
            rutas = rutas.stream()
                    .filter(r -> fecha.equals(r.getFecha()))
                    .collect(Collectors.toList());
        }
        return rutas;
    }

    public Ruta obtenerPorId(Long id) {
        return rutaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la ruta con id: " + id));
    }

    public boolean eliminar(Long id) {
        return rutaRepository.deleteById(id);
    }
}
