package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.*;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;

@RestController
@RequestMapping("/api/donaciones/entidad-beneficiaria")
public class EntidadBeneficiariaController {

    private final EntidadBeneficiariaService entidadBeneficiariaService;

    public EntidadBeneficiariaController(EntidadBeneficiariaService entidadBeneficiariaService) {
        this.entidadBeneficiariaService = entidadBeneficiariaService;
    }

    @GetMapping
    public ResponseEntity<List<ObtenerTodasEntidadesResponse>> obtenerTodas() {
        return ResponseEntity.ok(entidadBeneficiariaService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObtenerEntidadResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(entidadBeneficiariaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<CrearEntidadBeneficiariaResponse> crear(
            @RequestBody CrearEntidadBeneficiariaRequest request) {
        return ResponseEntity.ok(entidadBeneficiariaService.crear(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        boolean eliminado = entidadBeneficiariaService.eliminar(id);
        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActualizarEntidadBeneficiariaResponse> actualizar(@PathVariable Long id,
            @RequestBody ActualizarEntidadBeneficiariaRequest request) {
        return ResponseEntity.ok(entidadBeneficiariaService.actualizar(id, request));
    }

    @GetMapping("/{entidadId}/necesidades")
    public ResponseEntity<ObtenerNecesidadesResponse> obtenerNecesidades(@PathVariable Long entidadId) {
        return ResponseEntity.ok(entidadBeneficiariaService.obtenerNecesidades(entidadId));
    }

    @PostMapping("/{entidadId}/necesidades")
    public ResponseEntity<RegistrarNecesidadResponse> registrarNecesidad(@PathVariable Long entidadId,
            @RequestBody RegistrarNecesidadRequest request) {
        return ResponseEntity.ok(entidadBeneficiariaService.registrarNecesidad(entidadId, request));
    }

    @DeleteMapping("/{entidadId}/necesidades/{necesidadId}")
    public ResponseEntity<Void> eliminarNecesidad(@PathVariable Long entidadId,
            @PathVariable Long necesidadId) {
        entidadBeneficiariaService.eliminarNecesidad(entidadId, necesidadId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entidadId}/entregas/{donacionId}/confirmar")
    public ResponseEntity<Void> confirmarEntrega(@PathVariable Long entidadId, @PathVariable Long donacionId) {
        entidadBeneficiariaService.confirmarEntrega(entidadId, donacionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entidadId}/entregas/{donacionId}/no-recibida")
    public ResponseEntity<Void> reportarNoRecibida(@PathVariable Long entidadId, @PathVariable Long donacionId,
            @RequestBody ReportarNoRecibidaRequest request) {
        entidadBeneficiariaService.reportarNoRecibida(entidadId, donacionId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entidadId}/entregas/{donacionId}/fotos")
    public ResponseEntity<Void> subirFotosRecepcion(@PathVariable Long entidadId, @PathVariable Long donacionId,
            @RequestBody SubirFotosRecepcionRequest request) {
        entidadBeneficiariaService.subirFotosRecepcion(entidadId, donacionId, request);
        return ResponseEntity.noContent().build();
    }
}
