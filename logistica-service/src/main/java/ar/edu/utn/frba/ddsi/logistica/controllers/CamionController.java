package ar.edu.utn.frba.ddsi.logistica.controllers;

import ar.edu.utn.frba.ddsi.logistica.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.logistica.models.Camion;
import ar.edu.utn.frba.ddsi.logistica.repositories.CamionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/camiones")
public class CamionController {

    private final CamionRepository repository;

    public CamionController(CamionRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<CamionDTO> crear(@RequestBody CamionDTO dto) {
        Camion camion = new Camion();
        camion.setPatente(dto.getPatente());
        camion.setAltura(dto.getAltura());
        camion.setCapacidadVolumen(dto.getCapacidadVolumen());
        camion.setCapacidadCarga(dto.getCapacidadCarga());
        
        repository.guardar(camion);
        dto.setId(camion.getId());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CamionDTO>> obtenerTodos() {
        List<CamionDTO> dtos = repository.obtenerTodos().stream().map(c -> {
            CamionDTO dto = new CamionDTO();
            dto.setId(c.getId());
            dto.setPatente(c.getPatente());
            dto.setAltura(c.getAltura());
            dto.setCapacidadCarga(c.getCapacidadCarga());
            dto.setCapacidadVolumen(c.getCapacidadVolumen());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
