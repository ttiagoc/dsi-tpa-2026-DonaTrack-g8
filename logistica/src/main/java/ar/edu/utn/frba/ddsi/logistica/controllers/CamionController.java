package ar.edu.utn.frba.ddsi.logistica.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.logistica.dto.camion.ActualizarCamionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ActualizarCamionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.CrearCamionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.CrearCamionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ObtenerCamionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ObtenerTodosCamionesResponse;
import ar.edu.utn.frba.ddsi.logistica.services.CamionService;

@RestController
@RequestMapping("/api/logistica/camiones")
public class CamionController {

    private final CamionService camionService;

    public CamionController(CamionService camionService) {
        this.camionService = camionService;
    }

    @GetMapping
    public ResponseEntity<ObtenerTodosCamionesResponse> obtenerTodos() {
        return ResponseEntity.ok(camionService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObtenerCamionResponse> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(camionService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<CrearCamionResponse> crear(@RequestBody CrearCamionRequest request) {
        return ResponseEntity.ok(camionService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActualizarCamionResponse> actualizar(@PathVariable Long id, @RequestBody ActualizarCamionRequest request) {
        try {
            return ResponseEntity.ok(camionService.actualizar(id, request));
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
