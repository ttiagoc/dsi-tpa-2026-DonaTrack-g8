package ar.edu.utn.frba.ddsi.logistica.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.ObtenerUbicacionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.RecibirTelemetriaRequest;
import ar.edu.utn.frba.ddsi.logistica.services.MonitoreoService;

@RestController
@RequestMapping("/api/logistica/monitoreo")
public class MonitoreoController {

  private final MonitoreoService monitoreoService;

  public MonitoreoController(MonitoreoService monitoreoService) {
    this.monitoreoService = monitoreoService;
  }

  @PostMapping("/ubicacion/{patente}")
  public ResponseEntity<Void> recibirTelemetria(
      @PathVariable String patente,
      @RequestBody RecibirTelemetriaRequest request) {
    try {
      monitoreoService.actualizarUbicacionCamion(patente, request);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/ubicacion/{rutaId}")
  public ResponseEntity<ObtenerUbicacionResponse> obtenerUbicacionActual(@PathVariable Long rutaId) {
    try {
      ObtenerUbicacionResponse response = monitoreoService.obtenerUltimaUbicacionPorRuta(rutaId);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.notFound().build();
    }
  }

}
