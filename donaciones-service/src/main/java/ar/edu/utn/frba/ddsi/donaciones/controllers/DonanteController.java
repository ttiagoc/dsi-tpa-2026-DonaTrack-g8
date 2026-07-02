package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ar.edu.utn.frba.ddsi.donaciones.dto.donante.*;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;

@RestController
@RequestMapping("/donaciones/donantes")
public class DonanteController {

    private final DonanteService donanteService;

    public DonanteController(DonanteService donanteService) {
        this.donanteService = donanteService;
    }

    @GetMapping
    public ResponseEntity<List<DonanteResponse>> obtenerTodos() {
        return ResponseEntity.ok(donanteService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonanteResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(donanteService.obtenerPorId(id));
    }

    @PostMapping("/persona-humana")
    public ResponseEntity<DonanteResponse> crearPersonaHumana(
            @RequestBody PersonaHumanaRequest request) {
        return ResponseEntity.ok(donanteService.crearPersonaHumana(request));
    }

    @PostMapping("/persona-juridica")
    public ResponseEntity<DonanteResponse> crearPersonaJuridica(
            @RequestBody CrearPersonaJuridicaRequest request) {
        return ResponseEntity.ok(donanteService.crearPersonaJuridica(request));
    }

    @PutMapping("/persona-humana/{id}")
    public ResponseEntity<DonanteResponse> actualizarPersonaHumana(@PathVariable Long id,
            @RequestBody ActualizarPersonaHumanaRequest request) {
        return ResponseEntity.ok(donanteService.actualizarPersonaHumana(id, request));
    }

    @PutMapping("/persona-juridica/{id}")
    public ResponseEntity<DonanteResponse> actualizarPersonaJuridica(@PathVariable Long id,
            @RequestBody ActualizarPersonaJuridicaRequest request) {
        return ResponseEntity.ok(donanteService.actualizarPersonaJuridica(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (donanteService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
