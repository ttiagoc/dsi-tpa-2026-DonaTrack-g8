package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.*;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;

@RestController
@RequestMapping("/api/entidad-beneficiaria")
public class EntidadBeneficiariaController {

    private final EntidadBeneficiariaService entidadBeneficiariaService;

    public EntidadBeneficiariaController(EntidadBeneficiariaService entidadBeneficiariaService) {
        this.entidadBeneficiariaService = entidadBeneficiariaService;
    }

    @GetMapping
    public List<EntidadBeneficiariaResponse> obtenerTodas() {
        return entidadBeneficiariaService.obtenerTodas();
    }

    @GetMapping("/{id}")
    public EntidadBeneficiariaResponse obtenerPorId(@PathVariable Long id) {
        return entidadBeneficiariaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntidadBeneficiariaResponse crear(
            @RequestBody EntidadBeneficiariaRequest request) {
        return entidadBeneficiariaService.crear(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        entidadBeneficiariaService.eliminar(id);
    }

    @PutMapping("/{id}")
    public EntidadBeneficiariaResponse actualizar(@PathVariable Long id,
            @RequestBody EntidadBeneficiariaRequest request) {
        return entidadBeneficiariaService.actualizar(id, request);
    }

    @GetMapping("/{entidadId}/necesidades")
    public List<NecesidadResponse> obtenerNecesidades(@PathVariable Long entidadId) {
        return entidadBeneficiariaService.obtenerNecesidades(entidadId);
    }

    @PostMapping("/{entidadId}/necesidades")
    @ResponseStatus(HttpStatus.CREATED)
    public NecesidadResponse registrarNecesidad(@PathVariable Long entidadId,
            @RequestBody NecesidadRequest request) {
        return entidadBeneficiariaService.registrarNecesidad(entidadId, request);
    }

    @DeleteMapping("/{entidadId}/necesidades/{necesidadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarNecesidad(@PathVariable Long entidadId,
            @PathVariable Long necesidadId) {
        entidadBeneficiariaService.eliminarNecesidad(entidadId, necesidadId);
    }

    @GetMapping("/{id}/contactos")
    public List<MedioContacto> obtenerContactos(@PathVariable Long id) {
        return entidadBeneficiariaService.obtenerContactos(id);
    }
}
