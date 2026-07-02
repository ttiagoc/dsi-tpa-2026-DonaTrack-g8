package ar.edu.utn.frba.ddsi.donaciones.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.InicioRutaRequest;
import ar.edu.utn.frba.ddsi.donaciones.services.EventoService;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/evento")
@AllArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    @PostMapping("/inicio-ruta")
    public ResponseEntity<String> iniciarRuta(@RequestBody InicioRutaRequest request) {
        eventoService.iniciarRuta(request);
        return ResponseEntity.ok("Notificación de inicio de ruta enviada correctamente.");
    }

    @PostMapping("/confirmacion-entrega-exitosa")
    public ResponseEntity<String> confirmarEntregaExitosa(
            @RequestBody ConfirmacionEntregaExitosaRequest request) {
        eventoService.confirmarEntregaExitosa(request);
        return ResponseEntity.ok(
                "Notificación de confirmacion de entrega exitosa enviada correctamente.");
    }

}
