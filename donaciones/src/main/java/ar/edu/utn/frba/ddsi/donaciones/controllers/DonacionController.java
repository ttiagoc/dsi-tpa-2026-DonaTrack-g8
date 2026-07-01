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

import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.*;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;

@RestController
@RequestMapping("/api/donaciones/donacion")
public class DonacionController {

    private final DonacionService donacionService;

    public DonacionController(DonacionService donacionService) {
        this.donacionService = donacionService;
    }

    @GetMapping
    public ResponseEntity<List<ObtenerTodasDonacionesResponse>> obtenerTodas() {
        return ResponseEntity.ok(donacionService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObtenerDonacionResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(donacionService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<CrearDonacionResponse> crear(@RequestBody CrearDonacionRequest request) {
        return ResponseEntity.ok(donacionService.crear(request));
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
    public ResponseEntity<CambiarEstadoDonacionResponse> cambiarEstado(@PathVariable Long id,
            @RequestBody CambiarEstadoDonacionRequest request) {
        return ResponseEntity.ok(donacionService.cambiarEstado(id, request));
    }

    @GetMapping("/asignadas")
    public ResponseEntity<ObtenerDonacionesAsignadasResponse> obtenerDonacionesAsignadas(
            @RequestParam(name = "limit") int limit) {
        return ResponseEntity.ok(donacionService.obtenerDonacionesAsignadas(limit));
    }

    @PostMapping("/lista-entrega")
    public ResponseEntity<Void> donacionesEntregaLista(@RequestBody DonacionesListaEntregaRequest request) {
        donacionService.donacionesEntregaLista(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/replanificar")
    public ResponseEntity<ReplanificarDonacionResponse> replanificar(@PathVariable Long id) {
        return ResponseEntity.ok(donacionService.replanificar(id));
    }

}
