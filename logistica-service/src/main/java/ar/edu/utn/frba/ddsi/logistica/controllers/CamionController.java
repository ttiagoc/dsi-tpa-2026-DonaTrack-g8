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

import ar.edu.utn.frba.ddsi.logistica.dto.camion.CamionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.CamionResponse;
import ar.edu.utn.frba.ddsi.logistica.services.CamionService;

@RestController
@RequestMapping("/api/logistica/camiones")
public class CamionController {

    private final CamionService camionService;

    public CamionController(CamionService camionService) {
        this.camionService = camionService;
    }

    @GetMapping
    public ResponseEntity<List<CamionResponse>> obtenerTodos() {
        return ResponseEntity.ok(camionService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CamionResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(camionService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<CamionResponse> crear(@RequestBody CamionRequest request) {
        return ResponseEntity.ok(camionService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CamionResponse> actualizar(@PathVariable Long id, @RequestBody CamionRequest request) {
        return ResponseEntity.ok(camionService.actualizar(id, request));
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