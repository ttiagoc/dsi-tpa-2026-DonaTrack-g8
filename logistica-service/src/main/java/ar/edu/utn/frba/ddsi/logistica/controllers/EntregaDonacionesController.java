package ar.edu.utn.frba.ddsi.logistica.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.logistica.services.EntregaDonacionesService;

@RestController
@RequestMapping("/api/logistica-service/entregas")
public class EntregaDonacionesController {

    private final EntregaDonacionesService entregaDonacionesService;

    public EntregaDonacionesController(EntregaDonacionesService entregaDonacionesService) {
        this.entregaDonacionesService = entregaDonacionesService;
    }

    @PostMapping("/iniciar/{rutaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void iniciarRuta(@PathVariable Long rutaId) {
        entregaDonacionesService.iniciarRuta(rutaId);
    }

    @PostMapping("/confirmar/{paradaId}/{rutaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recibirConfirmacionEntregaExitosa(@PathVariable Long paradaId,
            @PathVariable Long rutaId) {
        entregaDonacionesService.confirmarEntregaExitosa(paradaId, rutaId);
    }
}
