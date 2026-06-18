package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/donaciones")
public class DonacionController {

    private final DonacionRepository donacionRepository;

    public DonacionController(DonacionRepository donacionRepository) {
        this.donacionRepository = donacionRepository;
    }

    @PostMapping
    public ResponseEntity<DonacionDTO> crearDonacion(@RequestBody DonacionDTO dto) {
        // En una implementacion real buscariamos el donante y los bienes
        // Aqui solo hacemos un mock basico para guardar
        Donacion donacion = new Donacion(null, dto.getFecha()); // Mock para que compile la logica base
        donacionRepository.guardar(donacion);
        
        dto.setId(donacion.getId());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DonacionDTO>> obtenerDonaciones() {
        List<DonacionDTO> dtos = donacionRepository.obtenerTodas().stream().map(d -> {
            DonacionDTO dto = new DonacionDTO();
            dto.setId(d.getId());
            dto.setFecha(d.getFecha());
            // Map state, etc.
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long id, @RequestBody String nuevoEstado) {
        Donacion donacion = donacionRepository.buscarPorId(id).orElse(null);
        if (donacion == null) return ResponseEntity.notFound().build();
        
        // Logica de cambio de estado
        return ResponseEntity.ok().build();
    }
}
