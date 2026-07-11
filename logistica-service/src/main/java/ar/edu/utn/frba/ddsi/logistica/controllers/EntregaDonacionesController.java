package ar.edu.utn.frba.ddsi.logistica.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.GestorDeRutas;

@RestController
@RequestMapping("/api/logistica-service/entregas")
public class EntregaDonacionesController {

    private final GestorDeRutas gestorDeRutas;

    public EntregaDonacionesController(GestorDeRutas gestorDeRutas) {
        this.gestorDeRutas = gestorDeRutas;
    }

    @PostMapping("/iniciar/{rutaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void iniciarRuta(@PathVariable Long rutaId) {
        gestorDeRutas.iniciarRuta(rutaId);
    }

    @PostMapping("/confirmar/{paradaId}/{rutaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recibirConfirmacionEntregaExitosa(@PathVariable Long paradaId,
            @PathVariable Long rutaId) {
        gestorDeRutas.confirmarEntregaExitosa(paradaId, rutaId);
    }
}
