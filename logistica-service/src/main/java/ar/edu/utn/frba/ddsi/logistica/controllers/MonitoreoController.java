package ar.edu.utn.frba.ddsi.logistica.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.CamionActivoResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionResponse;
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
      @RequestBody UbicacionRequest request) {
    monitoreoService.actualizarUbicacionCamion(patente, request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/ubicacion/{rutaId}")
  public ResponseEntity<UbicacionResponse> obtenerUbicacionActual(@PathVariable Long rutaId) {
    return ResponseEntity.ok(monitoreoService.obtenerUltimaUbicacionPorRuta(rutaId));
  }

  @GetMapping("/activos")
  public ResponseEntity<List<CamionActivoResponse>> obtenerCamionesActivos() {
    return ResponseEntity.ok(monitoreoService.obtenerCamionesActivos());
  }
}