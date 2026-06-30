package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;

@RestController
@RequestMapping("/api/donantes")
public class DonanteController {

    private final DonanteService donanteService;

    public DonanteController(DonanteService donanteService) {
        this.donanteService = donanteService;
    }

    @GetMapping
    public ResponseEntity<List<Donante>> obtenerTodos() {
        return ResponseEntity.ok(donanteService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Donante> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(donanteService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/persona-humana")
    public ResponseEntity<PersonaHumana> crearPersonaHumana(@RequestBody PersonaHumana persona) {
        return ResponseEntity.ok(donanteService.crearPersonaHumana(persona));
    }

    @PostMapping("/persona-juridica")
    public ResponseEntity<PersonaJuridica> crearPersonaJuridica(@RequestBody PersonaJuridica persona) {
        return ResponseEntity.ok(donanteService.crearPersonaJuridica(persona));
    }

    @PutMapping("/persona-humana/{id}")
    public ResponseEntity<PersonaHumana> actualizarPersonaHumana(@PathVariable Long id,
            @RequestBody PersonaHumana datosActualizados) {
        try {
            return ResponseEntity.ok(donanteService.actualizarPersonaHumana(id, datosActualizados));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/persona-juridica/{id}")
    public ResponseEntity<PersonaJuridica> actualizarPersonaJuridica(@PathVariable Long id,
            @RequestBody PersonaJuridica datosActualizados) {
        try {
            return ResponseEntity.ok(donanteService.actualizarPersonaJuridica(id, datosActualizados));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (donanteService.eliminar(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
