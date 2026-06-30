package ar.edu.utn.frba.ddsi.logistica.controllers;

import ar.edu.utn.frba.ddsi.logistica.models.entities.Ubicacion;
import ar.edu.utn.frba.ddsi.logistica.services.MonitoreoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/logistica/camiones")
public class MonitoreoController {

  private final MonitoreoService monitoreoService;

  public MonitoreoController(MonitoreoService monitoreoService) {
    this.monitoreoService = monitoreoService;
  }

  @PostMapping("/{patente}/ubicacion")
  public ResponseEntity<String> recibirTelemetria(
      @PathVariable String patente,
      @RequestBody Ubicacion ubicacion) {
    try {
      if(ubicacion.getTimestamp() == null) {
        ubicacion.setTimestamp(LocalDateTime.now());
      }
      monitoreoService.actualizarUbicacionCamion(patente, ubicacion);
      return ResponseEntity.ok("Ubicación actualizada correctamente.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
