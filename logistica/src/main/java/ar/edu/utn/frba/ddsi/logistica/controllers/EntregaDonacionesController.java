package ar.edu.utn.frba.ddsi.logistica.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.logistica.services.EntregaDonacionesService;

@RestController
@RequestMapping("/api/logistica/entregas")
public class EntregaDonacionesController {

    private final EntregaDonacionesService entregaDonacionesService;

    public EntregaDonacionesController(EntregaDonacionesService entregaDonacionesService) {
        this.entregaDonacionesService = entregaDonacionesService;
    }

    @PostMapping("/iniciar/{rutaId}")
    public ResponseEntity<Void> iniciarRuta(@PathVariable Long rutaId) {
        entregaDonacionesService.iniciarRuta(rutaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/confirmar/{paradaId}/{rutaId}")
    public ResponseEntity<Void> recibirConfirmacionEntregaExitosa(@PathVariable Long paradaId,
                                                                  @PathVariable Long rutaId) {
        entregaDonacionesService.confirmarEntregaExitosa(paradaId, rutaId);
        return ResponseEntity.noContent().build();
    }
}