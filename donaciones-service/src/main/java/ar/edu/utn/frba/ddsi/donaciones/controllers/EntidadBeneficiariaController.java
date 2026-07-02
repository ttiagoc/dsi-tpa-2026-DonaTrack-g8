package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
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
        return;
    }

    @PostMapping("/{entidadId}/entregas/{donacionId}/confirmar")
    @ResponseStatus(HttpStatus.CREATED)
    public void confirmarEntrega(@PathVariable Long entidadId, @PathVariable Long donacionId) {
        entidadBeneficiariaService.confirmarEntrega(entidadId, donacionId);
        return;
    }

    @PostMapping("/{entidadId}/entregas/{donacionId}/no-recibida")
    @ResponseStatus(HttpStatus.CREATED)
    public void reportarNoRecibida(@PathVariable Long entidadId, @PathVariable Long donacionId,
            @RequestBody ReportarNoRecibidaRequest request) {
        entidadBeneficiariaService.reportarNoRecibida(entidadId, donacionId, request);
        return;
    }

    @PostMapping("/{entidadId}/entregas/{donacionId}/fotos")
    @ResponseStatus(HttpStatus.CREATED)
    public void subirFotosRecepcion(@PathVariable Long entidadId, @PathVariable Long donacionId,
            @RequestBody SubirFotosRecepcionRequest request) {
        entidadBeneficiariaService.subirFotosRecepcion(entidadId, donacionId, request);
        return;
    }
}

