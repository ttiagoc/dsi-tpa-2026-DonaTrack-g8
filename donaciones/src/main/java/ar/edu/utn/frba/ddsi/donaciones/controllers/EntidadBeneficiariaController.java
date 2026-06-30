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
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.EventoService;

@RestController
@RequestMapping("/api/entidad-beneficiaria")
public class EntidadBeneficiariaController {

    private final EntidadBeneficiariaRepository entidadRepository;
    private final DonacionRepository donacionRepository;
    private final EventoService eventoService;

    public EntidadBeneficiariaController(EntidadBeneficiariaRepository entidadRepository,
            DonacionRepository donacionRepository,
            EventoService eventoService) {
        this.entidadRepository = entidadRepository;
        this.donacionRepository = donacionRepository;
        this.eventoService = eventoService;
    }

    @GetMapping
    public ResponseEntity<List<EntidadBeneficiaria>> obtenerTodas() {
        return ResponseEntity.ok(entidadRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntidadBeneficiaria> obtenerPorId(@PathVariable Long id) {
        return entidadRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EntidadBeneficiaria> crear(@RequestBody EntidadBeneficiaria entidad) {
        entidad.setId(null);
        EntidadBeneficiaria creada = entidadRepository.save(entidad);
        return ResponseEntity.ok(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntidadBeneficiaria> actualizar(@PathVariable Long id,
            @RequestBody EntidadBeneficiaria entidadActualizada) {
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEntidadBeneficiaria(@PathVariable Long id) {
      boolean eliminado = entidadRepository.deleteById(id);

      if (!eliminado) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.noContent().build();
    }

    @GetMapping("/{entidadId}/necesidades")
      public ResponseEntity<List<Necesidad>> obtenerNecesidades(@PathVariable Long entidadId) {
          return entidadRepository.findById(entidadId)
                  .map(entidad -> ResponseEntity.ok(entidad.getNecesidades()))
                  .orElse(ResponseEntity.notFound().build());
      }

    @PostMapping("/{entidadId}/necesidades")
    public ResponseEntity<Necesidad> registrarNecesidad(@PathVariable Long entidadId,
            @RequestBody Necesidad necesidad) {
        return entidadRepository.findById(entidadId)
                .map(entidad -> {
                    entidad.registrarNecesidad(necesidad);
                    entidadRepository.save(entidad);
                    return ResponseEntity.ok(necesidad);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // La lógica busca primero la entidad padre y después filtra la necesidad por subcategoría.
    @PutMapping("/{idEntidad}/necesidades/{subcategoriaNombre}")
    public ResponseEntity<Necesidad> actualizarNecesidad(
        @PathVariable Long idEntidad,
        @PathVariable String subcategoriaNombre,
        @RequestBody Necesidad datosNuevos) {

      var entidadOpt = entidadRepository.findById(idEntidad);
      if (entidadOpt.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      EntidadBeneficiaria entidad = entidadOpt.get();
      var necesidadOpt = entidad.getNecesidades().stream()
          .filter(n -> n.getSubcategoria() != null
              && subcategoriaNombre.equalsIgnoreCase(n.getSubcategoria().getNombre()))
          .findFirst();

      if (necesidadOpt.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      Necesidad necesidadVieja = necesidadOpt.get();
      necesidadVieja.setDescripcion(datosNuevos.getDescripcion());
      necesidadVieja.setCantidad(datosNuevos.getCantidad());
      necesidadVieja.setTipoNecesidad(datosNuevos.getTipoNecesidad());

      entidadRepository.save(entidad);

      return ResponseEntity.ok(necesidadVieja);
    }

    @DeleteMapping("/{entidadId}/necesidades/{subcategoriaNombre}")
    public ResponseEntity<Void> eliminarNecesidad(@PathVariable Long entidadId,
            @PathVariable String subcategoriaNombre) {
        return entidadRepository.findById(entidadId)
                .map(entidad -> {
                    boolean removido = entidad.getNecesidades().removeIf(n -> n.getSubcategoria() != null
                            && subcategoriaNombre.equalsIgnoreCase(n.getSubcategoria().getNombre()));
                    if (removido) {
                        entidadRepository.save(entidad);
                        return ResponseEntity.ok().<Void>build();
                    }
                    return ResponseEntity.notFound().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

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
                            return ResponseEntity
                                    .ok("Entrega de donación #" + donacionId + " confirmada con éxito por la entidad.");
                        })
                        .orElse(ResponseEntity.notFound().build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{entidadId}/entregas/{donacionId}/no-recibida")
    public ResponseEntity<String> reportarNoRecibida(@PathVariable Long entidadId, @PathVariable Long donacionId,
            @RequestParam String motivo) {
        return entidadRepository.findById(entidadId)
                .map(entidad -> donacionRepository.findById(donacionId)
                        .map(donacion -> {
                            if (donacion.getEntidadBeneficiariaAsignada() == null ||
                                    !donacion.getEntidadBeneficiariaAsignada().getId().equals(entidadId)) {
                                return ResponseEntity.badRequest().body("La donación no está asignada a esta entidad.");
                            }
                            eventoService.notificarEntregaFallida(donacionId, motivo);
                            return ResponseEntity
                                    .ok("Reporte de entrega fallida para donación #" + donacionId + " registrado.");
                        })
                        .orElse(ResponseEntity.notFound().build()))
                .orElse(ResponseEntity.notFound().build());
    }
}
