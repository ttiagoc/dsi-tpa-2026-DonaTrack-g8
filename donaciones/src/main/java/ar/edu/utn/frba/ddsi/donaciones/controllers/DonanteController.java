package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;

@RestController
@RequestMapping("/api/donantes")
public class DonanteController {

    private final DonanteRepository donanteRepository;

    public DonanteController(DonanteRepository donanteRepository) {
        this.donanteRepository = donanteRepository;
    }

    /**
     * 1. GET /api/donantes
     * Obtiene el listado completo de donantes (tanto humanas como jurídicas).
     */
    @GetMapping
    public ResponseEntity<List<Donante>> obtenerTodos() {
        return ResponseEntity.ok(donanteRepository.findAll());
    }

    /**
     * 2. GET /api/donantes/{id}
     * Obtiene el detalle de un donante específico por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Donante> obtenerPorId(@PathVariable Long id) {
        return donanteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 3. POST /api/donantes/persona-humana
     * Registra un nuevo donante de tipo Persona Humana.
     */
    @PostMapping("/persona-humana")
    public ResponseEntity<PersonaHumana> crearPersonaHumana(@RequestBody PersonaHumana persona) {
        persona.setId(null);
        PersonaHumana creada = (PersonaHumana) donanteRepository.save(persona);
        return ResponseEntity.ok(creada);
    }

    /**
     * 4. POST /api/donantes/persona-juridica
     * Registra un nuevo donante de tipo Persona Jurídica.
     */
    @PostMapping("/persona-juridica")
    public ResponseEntity<PersonaJuridica> crearPersonaJuridica(@RequestBody PersonaJuridica persona) {
        persona.setId(null);
        PersonaJuridica creada = (PersonaJuridica) donanteRepository.save(persona);
        return ResponseEntity.ok(creada);
    }

    /**
     * 5. PUT /api/donantes/persona-humana/{id}
     * Actualiza los datos de un donante Persona Humana existente.
     */
    @PutMapping("/persona-humana/{id}")
    public ResponseEntity<PersonaHumana> actualizarPersonaHumana(@PathVariable Long id, @RequestBody PersonaHumana datosActualizados) {
        return donanteRepository.findById(id)
                .filter(d -> d instanceof PersonaHumana)
                .map(d -> {
                    PersonaHumana persona = (PersonaHumana) d;
                    persona.setNombre(datosActualizados.getNombre());
                    persona.setApellido(datosActualizados.getApellido());
                    persona.setFechaNacimiento(datosActualizados.getFechaNacimiento());
                    persona.setDni(datosActualizados.getDni());
                    persona.setGenero(datosActualizados.getGenero());
                    persona.setDireccion(datosActualizados.getDireccion());
                    persona.setContactos(datosActualizados.getContactos());
                    persona.setContactoPredeterminado(datosActualizados.getContactoPredeterminado());
                    
                    PersonaHumana guardada = (PersonaHumana) donanteRepository.save(persona);
                    return ResponseEntity.ok(guardada);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 6. PUT /api/donantes/persona-juridica/{id}
     * Actualiza los datos de un donante Persona Jurídica existente.
     */
    @PutMapping("/persona-juridica/{id}")
    public ResponseEntity<PersonaJuridica> actualizarPersonaJuridica(@PathVariable Long id, @RequestBody PersonaJuridica datosActualizados) {
        return donanteRepository.findById(id)
                .filter(d -> d instanceof PersonaJuridica)
                .map(d -> {
                    PersonaJuridica persona = (PersonaJuridica) d;
                    persona.setRazonSocial(datosActualizados.getRazonSocial());
                    persona.setRubro(datosActualizados.getRubro());
                    persona.setTipo(datosActualizados.getTipo());
                    persona.setCuit(datosActualizados.getCuit());
                    persona.setRepresentantes(datosActualizados.getRepresentantes());
                    persona.setContactos(datosActualizados.getContactos());
                    persona.setContactoPredeterminado(datosActualizados.getContactoPredeterminado());
                    
                    PersonaJuridica guardada = (PersonaJuridica) donanteRepository.save(persona);
                    return ResponseEntity.ok(guardada);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 7. DELETE /api/donantes/{id}
     * Elimina un donante por su ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (donanteRepository.deleteById(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
