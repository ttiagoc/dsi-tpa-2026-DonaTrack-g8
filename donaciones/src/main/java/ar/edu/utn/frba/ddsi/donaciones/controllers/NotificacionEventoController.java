package ar.edu.utn.frba.ddsi.donaciones.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.donaciones.dto.EntregaExitosaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.InicioRutaDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.EventoService;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/notificacion-evento")
@AllArgsConstructor
public class NotificacionEventoController {

    private final EventoService eventoService;

    @PostMapping("/inicio-ruta")
    public ResponseEntity<String> iniciarRuta(@RequestBody InicioRutaDTO dto) {
        try {
            eventoService.iniciarRuta(dto);
            return ResponseEntity.ok("Notificación de inicio de ruta enviada correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/confirmacion-entrega-exitosa")
    public ResponseEntity<String> confirmarEntregaExitosa(@RequestBody EntregaExitosaDTO dto) {
        try {
            eventoService.confirmarEntregaExitosa(dto);
            return ResponseEntity.ok("Notificación de confirmacion de entrega exitosa enviada correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
