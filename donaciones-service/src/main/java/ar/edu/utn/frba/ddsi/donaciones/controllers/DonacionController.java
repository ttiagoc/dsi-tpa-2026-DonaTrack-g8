package ar.edu.utn.frba.ddsi.donaciones.controllers;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionAsignadaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;

@RestController
@RequestMapping("/api/donaciones/donacion")
public class DonacionController {

    private final DonacionService donacionService;

    public DonacionController(DonacionService donacionService) {
        this.donacionService = donacionService;
    }

    @GetMapping
    public List<DonacionResponse> obtenerTodas() {
        return donacionService.obtenerTodas();
    }

    @GetMapping("/{id}")
    public DonacionResponse obtenerPorId(@PathVariable Long id) {
        return donacionService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<DonacionResponse> crear(@RequestBody DonacionRequest request) {
        return donacionService.crear(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        donacionService.eliminar(id);
        
    }

    @PutMapping("/estado/{id}")
    public EstadoDonacionResponse cambiarEstado(@PathVariable Long id,
            @RequestBody EstadoDonacionRequest request) {
        return donacionService.cambiarEstado(id, request);
    }

    @GetMapping("/asignadas")
    public List<DonacionAsignadaResponse> obtenerDonacionesAsignadas(
            @RequestParam(name = "limit") int limit) {
        return donacionService.obtenerDonacionesAsignadas(limit);
    }

    @PostMapping("/lista-entrega")
    @ResponseStatus(HttpStatus.CREATED)
    public void donacionesEntregaLista(@RequestBody List<Long> donacionesIds) {
        donacionService.donacionesEntregaLista(donacionesIds);
        return;
    }

    @PutMapping("/{id}/replanificar")
    public void replanificar(@PathVariable Long id) {
        donacionService.replanificar(id);
        return;
    }

}

