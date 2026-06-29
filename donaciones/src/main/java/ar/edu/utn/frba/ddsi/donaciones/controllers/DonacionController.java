package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.donaciones.dto.CambioEstadoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;

@RestController
@RequestMapping("/api/donacion")
public class DonacionController {

    private final DonacionService donacionService;

    public DonacionController(DonacionService donacionService) {
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

    @GetMapping
    public ResponseEntity<List<Donacion>> obtenerTodas() {
        try {
            List<Donacion> donaciones = donacionService.obtenerTodas();
            return ResponseEntity.ok(donaciones);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Donacion> obtenerPorId(@PathVariable Long id) {
        try {
            Donacion donacion = donacionService.obtenerPorId(id);
            return ResponseEntity.ok(donacion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<Donacion> crearDonacion(@RequestBody Donacion donacion) {
        try {
            Donacion nuevaDonacion = donacionService.crear(donacion);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaDonacion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarDonacion(@PathVariable Long id) {
        try {
            donacionService.eliminar(id);
            return ResponseEntity.ok("Donación eliminada correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/estado")
    public ResponseEntity<String> cambiarEstado(
            @RequestBody CambioEstadoDTO cambioEstadoDTO) {
        try {
            donacionService.cambiarEstado(cambioEstadoDTO.getDonacionId(), cambioEstadoDTO.getNuevoEstado(),
                    cambioEstadoDTO.getJustificacion());
            return ResponseEntity.ok("Estado de la donación actualizado y auditado correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error en la solicitud: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno: " + e.getMessage());
        }
    }

}
