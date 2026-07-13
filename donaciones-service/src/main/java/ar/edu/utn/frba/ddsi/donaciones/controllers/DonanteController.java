package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.DonanteResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.GestorDeEventos;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;

@RestController
@RequestMapping("/api/donantes")
public class DonanteController {

    private final DonanteService donanteService;
    private final GestorDeEventos gestorDeEventos;

    public DonanteController(DonanteService donanteService, GestorDeEventos gestorDeEventos) {
        this.donanteService = donanteService;
        this.gestorDeEventos = gestorDeEventos;
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
            @RequestBody PersonaJuridicaRequest request) {
        return donanteService.crearPersonaJuridica(request);
    }

    @PutMapping("/persona-humana/{id}")
    public DonanteResponse actualizarPersonaHumana(@PathVariable Long id,
            @RequestBody PersonaHumanaRequest request) {
        return donanteService.actualizarPersonaHumana(id, request);
    }

    @PutMapping("/persona-juridica/{id}")
    public DonanteResponse actualizarPersonaJuridica(@PathVariable Long id,
            @RequestBody PersonaJuridicaRequest request) {
        return donanteService.actualizarPersonaJuridica(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        donanteService.eliminar(id);
    }

    @GetMapping("/{id}/contacto")
    public MedioContacto obtenerContactoPredeterminado(@PathVariable Long id) {
        return donanteService.obtenerContactoPredeterminado(id);
    }

    @PostMapping("/inactividad/verificaciones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verificarInactividad() {
        gestorDeEventos.verificarInactividadDonantes();
    }
}
