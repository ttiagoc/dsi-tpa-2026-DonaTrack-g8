package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.NecesidadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/necesidades")
public class NecesidadController {

    private final NecesidadRepository repository;

    public NecesidadController(NecesidadRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<NecesidadDTO> crear(@RequestBody NecesidadDTO dto) {
        Necesidad necesidad = new Necesidad(null, null, dto.getDescripcion(), dto.getCantidad());
        repository.guardar(necesidad);
        dto.setId(necesidad.getId());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<NecesidadDTO>> obtenerTodas() {
        List<NecesidadDTO> dtos = repository.obtenerTodas().stream().map(n -> {
            NecesidadDTO dto = new NecesidadDTO();
            dto.setId(n.getId());
            dto.setDescripcion(n.getDescripcion());
            dto.setCantidad(n.getCantidad());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
