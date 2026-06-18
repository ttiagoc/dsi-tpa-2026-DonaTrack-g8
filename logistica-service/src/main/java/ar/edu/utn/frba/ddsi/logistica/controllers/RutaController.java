package ar.edu.utn.frba.ddsi.logistica.controllers;

import ar.edu.utn.frba.ddsi.logistica.dto.RutaDTO;
import ar.edu.utn.frba.ddsi.logistica.models.Ruta;
import ar.edu.utn.frba.ddsi.logistica.repositories.RutaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rutas")
public class RutaController {

    private final RutaRepository repository;

    public RutaController(RutaRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<RutaDTO> crear(@RequestBody RutaDTO dto) {
        Ruta ruta = new Ruta();
        ruta.setFecha(dto.getFecha());
        ruta.setEstado("PLANIFICADA");
        ruta.setChoferNombre(dto.getChoferNombre());
        
        repository.guardar(ruta);
        dto.setId(ruta.getId());
        dto.setEstado(ruta.getEstado());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RutaDTO>> obtenerTodas() {
        List<RutaDTO> dtos = repository.obtenerTodas().stream().map(r -> {
            RutaDTO dto = new RutaDTO();
            dto.setId(r.getId());
            dto.setFecha(r.getFecha());
            dto.setEstado(r.getEstado());
            dto.setChoferNombre(r.getChoferNombre());
            if (r.getCamion() != null) {
                dto.setCamionId(r.getCamion().getId());
            }
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
