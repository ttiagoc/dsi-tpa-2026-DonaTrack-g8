package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/entidades")
public class EntidadBeneficiariaController {

    private final EntidadBeneficiariaRepository repository;

    public EntidadBeneficiariaController(EntidadBeneficiariaRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<EntidadBeneficiariaDTO> crear(@RequestBody EntidadBeneficiariaDTO dto) {
        EntidadBeneficiaria entidad = new EntidadBeneficiaria(dto.getRazonSocial(), dto.getDireccion(), null, null);
        repository.guardar(entidad);
        dto.setId(entidad.getId());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EntidadBeneficiariaDTO>> obtenerTodas() {
        List<EntidadBeneficiariaDTO> dtos = repository.obtenerTodas().stream().map(e -> {
            EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
            dto.setId(e.getId());
            dto.setRazonSocial(e.getRazonSocial());
            dto.setDireccion(e.getDireccion());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
