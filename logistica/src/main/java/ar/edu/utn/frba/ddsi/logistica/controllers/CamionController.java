package ar.edu.utn.frba.ddsi.logistica.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.services.CamionService;

@RestController
@RequestMapping("/api/camiones")
public class CamionController {

    private final CamionService camionService;

    public CamionController(CamionService camionService) {
        this.camionService = camionService;
    }

    @GetMapping
    public ResponseEntity<List<Camion>> obtenerTodos() {
        return ResponseEntity.ok(camionService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Camion> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(camionService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Camion> crear(@RequestBody Camion camion) {
        return ResponseEntity.ok(camionService.crear(camion));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Camion> actualizar(@PathVariable Long id, @RequestBody Camion camion) {
        try {
            return ResponseEntity.ok(camionService.actualizar(id, camion));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        boolean eliminado = camionService.eliminar(id);
        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
