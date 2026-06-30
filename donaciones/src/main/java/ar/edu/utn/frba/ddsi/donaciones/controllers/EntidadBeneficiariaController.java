package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;

@RestController
@RequestMapping("/api/entidad-beneficiaria")
public class EntidadBeneficiariaController {

    private final EntidadBeneficiariaService entidadBeneficiariaService;

    public EntidadBeneficiariaController(EntidadBeneficiariaService entidadBeneficiariaService) {
        this.entidadBeneficiariaService = entidadBeneficiariaService;
    }

    @GetMapping
    public ResponseEntity<List<EntidadBeneficiaria>> obtenerTodas() {
        return ResponseEntity.ok(entidadBeneficiariaService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntidadBeneficiaria> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(entidadBeneficiariaService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<EntidadBeneficiaria> crear(@RequestBody EntidadBeneficiaria entidad) {
        return ResponseEntity.ok(entidadBeneficiariaService.crear(entidad));
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
    public ResponseEntity<EntidadBeneficiaria> actualizar(@PathVariable Long id,
            @RequestBody EntidadBeneficiaria entidadActualizada) {
        try {
            return ResponseEntity.ok(entidadBeneficiariaService.actualizar(id, entidadActualizada));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{entidadId}/necesidades")
    public ResponseEntity<List<Necesidad>> obtenerNecesidades(@PathVariable Long entidadId) {
        try {
            return ResponseEntity.ok(entidadBeneficiariaService.obtenerNecesidades(entidadId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{entidadId}/necesidades")
    public ResponseEntity<Necesidad> registrarNecesidad(@PathVariable Long entidadId,
            @RequestBody Necesidad necesidad) {
        try {
            return ResponseEntity.ok(entidadBeneficiariaService.registrarNecesidad(entidadId, necesidad));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{entidadId}/necesidades/{necesidadId}")
    public ResponseEntity<Void> eliminarNecesidad(@PathVariable Long entidadId,
            @PathVariable Long necesidadId) {
        try {
            entidadBeneficiariaService.eliminarNecesidad(entidadId, necesidadId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{entidadId}/entregas/{donacionId}/confirmar")
    public ResponseEntity<Void> confirmarEntrega(@PathVariable Long entidadId, @PathVariable Long donacionId) {
        try {
            entidadBeneficiariaService.confirmarEntrega(entidadId, donacionId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{entidadId}/entregas/{donacionId}/no-recibida")
    public ResponseEntity<String> reportarNoRecibida(@PathVariable Long entidadId, @PathVariable Long donacionId,
            @RequestParam String motivo) {
        try {
            entidadBeneficiariaService.reportarNoRecibida(entidadId, donacionId, motivo);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{entidadId}/entregas/{donacionId}/fotos")
    public ResponseEntity<Void> subirFotosRecepcion(@PathVariable Long entidadId, @PathVariable Long donacionId,
            @RequestBody List<String> fotosUrl) {
        try {
            entidadBeneficiariaService.subirFotosRecepcion(entidadId, donacionId, fotosUrl);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
