package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

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

import ar.edu.utn.frba.ddsi.donaciones.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.CambioEstado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;

@RestController
@RequestMapping("/api/donacion")
public class DonacionController {

    private final DonacionService donacionService;
    private final DonacionRepository donacionRepository;

    public DonacionController(DonacionService donacionService, DonacionRepository donacionRepository) {
        this.donacionService = donacionService;
        this.donacionRepository = donacionRepository;
    }

    @GetMapping
    public ResponseEntity<List<Donacion>> obtenerTodas() {
        return ResponseEntity.ok(donacionService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Donacion> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(donacionService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<List<Donacion>> crear(@RequestBody RegistroDonacion registroDonacion) {
        return ResponseEntity.ok(donacionService.crear(registroDonacion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        boolean eliminado = donacionService.eliminar(id);
        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/estado/{id}")
    public ResponseEntity<Donacion> cambiarEstado(@PathVariable Long id,
            @RequestBody CambioEstado cambioEstado) {
        try {
            donacionService.cambiarEstado(id, cambioEstado.getEstado(), cambioEstado.getJustificacion());
            return ResponseEntity.ok(donacionRepository.findById(id).orElse(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/asignadas")
    public ResponseEntity<List<DonacionDTO>> obtenerDonacionesAsignadas(
            @RequestParam(name = "limit") int limit) {
        try {
            return ResponseEntity.ok(donacionService.obtenerDonacionesAsignadas(limit));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/lista-entrega")
    public ResponseEntity<Void> donacionesEntregaLista(@RequestBody List<DonacionDTO> donaciones) {
        donacionService.donacionesEntregaLista(donaciones);
        return ResponseEntity.noContent().build();
    }

}
