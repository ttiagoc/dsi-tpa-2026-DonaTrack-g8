package ar.edu.utn.frba.ddsi.logistica.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
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
@RequestMapping("/api/logistica-service/camiones")
public class CamionController {

    private final CamionService camionService;

    public CamionController(CamionService camionService) {
        this.camionService = camionService;
    }

    @GetMapping
    public List<CamionResponse> obtenerTodos() {
        return camionService.obtenerTodos();
    }

    @GetMapping("/{id}")
    public CamionResponse obtenerPorId(@PathVariable Long id) {
        return camionService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CamionResponse crear(@RequestBody CamionRequest request) {
        return camionService.crear(request);
    }

    @PutMapping("/{id}")
    public CamionResponse actualizar(@PathVariable Long id, @RequestBody CamionRequest request) {
        return camionService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        camionService.eliminar(id);
    }
}
