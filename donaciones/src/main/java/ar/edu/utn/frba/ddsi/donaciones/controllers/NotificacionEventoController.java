package ar.edu.utn.frba.ddsi.donaciones.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.donaciones.services.NotificacionEventoService;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/notificacion-evento")
@AllArgsConstructor
public class NotificacionEventoController {

    private final NotificacionEventoService notificacionEventoService;

    @PostMapping("/inicio-ruta-donante/{donacion}")
    public ResponseEntity<String> notificarInicioRutaDonante(@PathVariable Long donacion) {
        try {
            notificacionEventoService.notificarInicioRutaDonante(donacion);
            return ResponseEntity.ok("Notificación de inicio de ruta enviada correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/inicio-ruta-entidad/{entidad}")
    public ResponseEntity<String> notificarInicioRutaEntidad(@PathVariable Long entidad) {
        try {
            notificacionEventoService.notificarInicioRutaEntidad(entidad);
            return ResponseEntity.ok("Notificación de inicio de ruta enviada correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/confirmacion-entrega-exitosa/{donacionId}")
    public ResponseEntity<String> notificarConfirmacionEntregaExitosa(@PathVariable Long donacionId) {
        try {
            notificacionEventoService.notificarConfirmacionEntregaExitosa(donacionId);
            return ResponseEntity.ok("Notificación de confirmacion de entrega exitosa enviada correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
