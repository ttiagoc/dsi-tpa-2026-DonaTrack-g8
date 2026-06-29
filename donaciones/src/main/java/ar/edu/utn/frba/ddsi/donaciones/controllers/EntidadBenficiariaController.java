package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;

@RestController
@RequestMapping("/api/entidad-beneficiaria")
public class EntidadBenficiariaController {

    private final EntidadBeneficiariaRepository entidadRepository;
    private final DonacionRepository donacionRepository;

    public EntidadBenficiariaController(EntidadBeneficiariaRepository entidadRepository, DonacionRepository donacionRepository) {
        this.entidadRepository = entidadRepository;
        this.donacionRepository = donacionRepository;
    }

    /**
     * 1. GET /api/entidad-beneficiaria
     * Obtiene el listado de todas las entidades beneficiarias registradas.
     */
    @GetMapping
    public ResponseEntity<List<EntidadBeneficiaria>> obtenerTodas() {
        return ResponseEntity.ok(entidadRepository.findAll());
    }

    /**
     * 2. GET /api/entidad-beneficiaria/{id}
     * Obtiene el detalle de una entidad beneficiaria por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntidadBeneficiaria> obtenerPorId(@PathVariable Long id) {
        return entidadRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 3. POST /api/entidad-beneficiaria
     * Registra una nueva entidad beneficiaria.
     */
    @PostMapping
    public ResponseEntity<EntidadBeneficiaria> crear(@RequestBody EntidadBeneficiaria entidad) {
        entidad.setId(null); // Evitamos pisar registros existentes
        EntidadBeneficiaria creada = entidadRepository.save(entidad);
        return ResponseEntity.ok(creada);
    }

    /**
     * 4. PUT /api/entidad-beneficiaria/{id}
     * Actualiza los datos de una entidad beneficiaria existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EntidadBeneficiaria> actualizar(@PathVariable Long id, @RequestBody EntidadBeneficiaria entidadActualizada) {
        return entidadRepository.findById(id)
                .map(entidadExistente -> {
                    entidadExistente.setRazonSocial(entidadActualizada.getRazonSocial());
                    entidadExistente.setDireccion(entidadActualizada.getDireccion());
                    entidadExistente.setTelefono(entidadActualizada.getTelefono());
                    entidadExistente.setCorreoRepresentantes(entidadActualizada.getCorreoRepresentantes());
                    
                    EntidadBeneficiaria guardada = entidadRepository.save(entidadExistente);
                    return ResponseEntity.ok(guardada);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 5. GET /api/entidad-beneficiaria/{entidadId}/necesidades
     * Obtiene el listado de necesidades de una entidad beneficiaria.
     */
    @GetMapping("/{entidadId}/necesidades")
    public ResponseEntity<List<Necesidad>> obtenerNecesidades(@PathVariable Long entidadId) {
        return entidadRepository.findById(entidadId)
                .map(entidad -> ResponseEntity.ok(entidad.getNecesidades()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 6. POST /api/entidad-beneficiaria/{entidadId}/necesidades
     * Registra una nueva necesidad material para una entidad beneficiaria.
     */
    @PostMapping("/{entidadId}/necesidades")
    public ResponseEntity<Necesidad> registrarNecesidad(@PathVariable Long entidadId, @RequestBody Necesidad necesidad) {
        return entidadRepository.findById(entidadId)
                .map(entidad -> {
                    entidad.registrarNecesidad(necesidad);
                    entidadRepository.save(entidad);
                    return ResponseEntity.ok(necesidad);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 7. DELETE /api/entidad-beneficiaria/{entidadId}/necesidades/{subcategoriaNombre}
     * Elimina una necesidad material de una entidad beneficiaria por el nombre de la subcategoría del bien.
     */
    @DeleteMapping("/{entidadId}/necesidades/{subcategoriaNombre}")
    public ResponseEntity<Void> eliminarNecesidad(@PathVariable Long entidadId, @PathVariable String subcategoriaNombre) {
        return entidadRepository.findById(entidadId)
                .map(entidad -> {
                    boolean removido = entidad.getNecesidades().removeIf(n -> 
                        n.getSubcategoria() != null && subcategoriaNombre.equalsIgnoreCase(n.getSubcategoria().getNombre())
                    );
                    if (removido) {
                        entidadRepository.save(entidad);
                        return ResponseEntity.ok().<Void>build();
                    }
                    return ResponseEntity.notFound().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 8. POST /api/entidad-beneficiaria/{entidadId}/entregas/{donacionId}/confirmar
     * Confirma la entrega exitosa de una donación asignada a la entidad.
     */
    @PostMapping("/{entidadId}/entregas/{donacionId}/confirmar")
    public ResponseEntity<String> confirmarEntrega(@PathVariable Long entidadId, @PathVariable Long donacionId) {
        return entidadRepository.findById(entidadId)
                .map(entidad -> donacionRepository.findById(donacionId)
                        .map(donacion -> {
                            if (donacion.getEntidadBeneficiariaAsignada() == null ||
                                    !donacion.getEntidadBeneficiariaAsignada().getId().equals(entidadId)) {
                                return ResponseEntity.badRequest().body("La donación no está asignada a esta entidad.");
                            }
                            
                            entidad.confirmarEntrega(donacion);
                            donacionRepository.save(donacion);
                            return ResponseEntity.ok("Entrega de donación #" + donacionId + " confirmada con éxito por la entidad.");
                        })
                        .orElse(ResponseEntity.notFound().build()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 9. POST /api/entidad-beneficiaria/{entidadId}/entregas/{donacionId}/no-recibida
     * Informa que la entrega no fue recibida por la entidad en el día correspondiente.
     */
    @PostMapping("/{entidadId}/entregas/{donacionId}/no-recibida")
    public ResponseEntity<String> reportarNoRecibida(@PathVariable Long entidadId, @PathVariable Long donacionId, @RequestParam String motivo) {
        return entidadRepository.findById(entidadId)
                .map(entidad -> donacionRepository.findById(donacionId)
                        .map(donacion -> {
                            if (donacion.getEntidadBeneficiariaAsignada() == null ||
                                    !donacion.getEntidadBeneficiariaAsignada().getId().equals(entidadId)) {
                                return ResponseEntity.badRequest().body("La donación no está asignada a esta entidad.");
                            }
                            
                            donacion.cambiarEstado(TipoEstadoDonacion.ENTREGA_FALLIDA, "Reportada como no recibida por la entidad. Motivo: " + motivo);
                            donacionRepository.save(donacion);
                            return ResponseEntity.ok("Reporte de entrega fallida para donación #" + donacionId + " registrado.");
                        })
                        .orElse(ResponseEntity.notFound().build()))
                .orElse(ResponseEntity.notFound().build());
    }
}
