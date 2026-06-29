package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;

@RestController
@RequestMapping("/api/donaciones")
public class DonacionesController {

    private final DonacionService donacionService;

    public DonacionesController(DonacionService donacionService) {
        this.donacionService = donacionService;
    }

    @GetMapping("/asignadas")
    public ResponseEntity<List<DonacionDTO>> obtenerDonacionesAsignadas(
            @RequestParam(name = "limit") int limit) {
        try {
            return ResponseEntity.ok(donacionService.obtenerDonacionesAsignadas(limit));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/lista-entrega")
    public ResponseEntity<String> donacionesEntregaLista(@RequestBody List<DonacionDTO> donaciones) {
        try {
            donacionService.donacionesEntregaLista(donaciones);
            return ResponseEntity.ok("Rutas planificadas correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

}
