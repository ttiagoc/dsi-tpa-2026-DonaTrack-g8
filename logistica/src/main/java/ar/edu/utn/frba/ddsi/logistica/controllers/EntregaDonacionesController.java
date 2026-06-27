package ar.edu.utn.frba.ddsi.logistica.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.logistica.services.EntregaDonacionesService;

@RestController
@RequestMapping("/api/donaciones/eventos")
public class EntregaDonacionesController {

    private final EntregaDonacionesService entregaDonacionesService;

    public EntregaDonacionesController(EntregaDonacionesService entregaDonacionesService) {
        this.entregaDonacionesService = entregaDonacionesService;
    }

    @PostMapping("/inicio-ruta/{rutaId}")
    public ResponseEntity<Void> recibirInicioRuta(@PathVariable Long rutaId) {
        entregaDonacionesService.iniciarRuta(rutaId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirmacion-entrega-exitosa/{donacionId}")
    public ResponseEntity<Void> recibirConfirmacionEntregaExitosa(@PathVariable Long donacionId) {
        entregaDonacionesService.confirmarEntregaExitosa(donacionId);
        return ResponseEntity.ok().build();
    }
}