package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;

@Service
public class DonanteService {

    private final DonanteRepository donanteRepository;

    public DonanteService(DonanteRepository donanteRepository) {
        this.donanteRepository = donanteRepository;
    }

    public List<Donante> obtenerTodos() {
        return donanteRepository.findAll();
    }

    public Donante obtenerPorId(Long id) {
        return donanteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el donante"));
    }

    public PersonaHumana crearPersonaHumana(PersonaHumana persona) {
        persona.setId(null);
        return (PersonaHumana) donanteRepository.save(persona);
    }

    public PersonaJuridica crearPersonaJuridica(PersonaJuridica persona) {
        persona.setId(null);
        return (PersonaJuridica) donanteRepository.save(persona);
    }

    public PersonaHumana actualizarPersonaHumana(Long id, PersonaHumana datosActualizados) {
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

                    return (PersonaHumana) donanteRepository.save(persona);
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el donante Persona Humana"));
    }

    public PersonaJuridica actualizarPersonaJuridica(Long id, PersonaJuridica datosActualizados) {
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

                    return (PersonaJuridica) donanteRepository.save(persona);
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el donante Persona Juridica"));
    }

    public boolean eliminar(Long id) {
        return donanteRepository.deleteById(id);
    }
}
