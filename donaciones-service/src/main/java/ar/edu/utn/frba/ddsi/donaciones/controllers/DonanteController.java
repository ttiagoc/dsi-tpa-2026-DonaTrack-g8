package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    public List<DonanteResponse> obtenerTodos() {
        return donanteService.obtenerTodos();
    }

    @GetMapping("/{id}")
    public DonanteResponse obtenerPorId(@PathVariable Long id) {
        return donanteService.obtenerPorId(id);
    }

    @PostMapping("/persona-humana")
    @ResponseStatus(HttpStatus.CREATED)
    public DonanteResponse crearPersonaHumana(
            @RequestBody PersonaHumanaRequest request) {
        return donanteService.crearPersonaHumana(request);
    }

    @PostMapping("/persona-juridica")
    @ResponseStatus(HttpStatus.CREATED)
    public DonanteResponse crearPersonaJuridica(
            @RequestBody CrearPersonaJuridicaRequest request) {
        return donanteService.crearPersonaJuridica(request);
    }

    @PutMapping("/persona-humana/{id}")
    public DonanteResponse actualizarPersonaHumana(@PathVariable Long id,
            @RequestBody ActualizarPersonaHumanaRequest request) {
        return donanteService.actualizarPersonaHumana(id, request);
    }

    @PutMapping("/persona-juridica/{id}")
    public DonanteResponse actualizarPersonaJuridica(@PathVariable Long id,
            @RequestBody ActualizarPersonaJuridicaRequest request) {
        return donanteService.actualizarPersonaJuridica(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        donanteService.eliminar(id);
    }
}

