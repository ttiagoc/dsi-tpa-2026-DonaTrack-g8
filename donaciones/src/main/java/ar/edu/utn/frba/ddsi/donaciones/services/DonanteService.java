package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Telefono;
import ar.edu.utn.frba.ddsi.common.models.entities.WhatsApp;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;

@Service
public class DonanteService {

    private final DonanteRepository donanteRepository;

    public DonanteService(DonanteRepository donanteRepository) {
        this.donanteRepository = donanteRepository;
    }

    public List<ObtenerTodosDonanteResponse> obtenerTodos() {
        return donanteRepository.findAll().stream()
                .map(d -> new ObtenerTodosDonanteResponse(
                        d.getId(),
                        d instanceof PersonaHumana ? "Persona Humana" : "Persona Jurídica",
                        d instanceof PersonaHumana ? ((PersonaHumana) d).getNombre() + " " + ((PersonaHumana) d).getApellido() : ((PersonaJuridica) d).getRazonSocial()
                ))
                .collect(Collectors.toList());
    }

    public ObtenerDonanteResponse obtenerPorId(Long id) {
        Donante d = donanteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el donante"));
        
        List<MedioContactoInfo> contactosInfo = d.getContactos() != null ? 
                d.getContactos().stream().map(this::parseContactoInfo).collect(Collectors.toList()) : new ArrayList<>();
        
        String tipo = d instanceof PersonaHumana ? "Persona Humana" : "Persona Jurídica";
        String nombre = d instanceof PersonaHumana ? ((PersonaHumana) d).getNombre() + " " + ((PersonaHumana) d).getApellido() : ((PersonaJuridica) d).getRazonSocial();
        
        return new ObtenerDonanteResponse(d.getId(), tipo, nombre, contactosInfo);
    }

    public CrearPersonaHumanaResponse crearPersonaHumana(CrearPersonaHumanaRequest request) {
        PersonaHumana persona = new PersonaHumana();
        persona.setNombre(request.nombre());
        persona.setApellido(request.apellido());
        persona.setFechaNacimiento(request.fechaNacimiento());
        persona.setDni(request.dni());
        persona.setGenero(request.genero());
        persona.setDireccion(request.direccion());
        if (request.contactos() != null) {
            persona.setContactos(request.contactos().stream().map(this::parseContacto).collect(Collectors.toList()));
        }
        
        persona = (PersonaHumana) donanteRepository.save(persona);
        return new CrearPersonaHumanaResponse(persona.getId(), persona.getNombre(), persona.getApellido(), persona.getDni());
    }

    public CrearPersonaJuridicaResponse crearPersonaJuridica(CrearPersonaJuridicaRequest request) {
        PersonaJuridica persona = new PersonaJuridica();
        persona.setRazonSocial(request.razonSocial());
        persona.setRubro(request.rubro());
        persona.setTipo(request.tipo());
        persona.setCuit(request.cuit());
        
        if (request.representantes() != null) {
            List<Representante> representantes = request.representantes().stream()
                    .map(r -> new Representante(r.nombre(), r.apellido(), new Email(r.correo())))
                    .collect(Collectors.toList());
            persona.setRepresentantes(representantes);
        }
        
        persona = (PersonaJuridica) donanteRepository.save(persona);
        return new CrearPersonaJuridicaResponse(persona.getId(), persona.getRazonSocial(), persona.getCuit());
    }

    public ActualizarPersonaHumanaResponse actualizarPersonaHumana(Long id, ActualizarPersonaHumanaRequest request) {
        return donanteRepository.findById(id)
                .filter(d -> d instanceof PersonaHumana)
                .map(d -> {
                    PersonaHumana persona = (PersonaHumana) d;
                    persona.setNombre(request.nombre());
                    persona.setApellido(request.apellido());
                    persona.setFechaNacimiento(request.fechaNacimiento());
                    persona.setDni(request.dni());
                    persona.setGenero(request.genero());
                    persona.setDireccion(request.direccion());
                    
                    persona = (PersonaHumana) donanteRepository.save(persona);
                    return new ActualizarPersonaHumanaResponse(persona.getId(), persona.getNombre(), persona.getApellido(), persona.getDni());
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el donante Persona Humana"));
    }

    public ActualizarPersonaJuridicaResponse actualizarPersonaJuridica(Long id, ActualizarPersonaJuridicaRequest request) {
        return donanteRepository.findById(id)
                .filter(d -> d instanceof PersonaJuridica)
                .map(d -> {
                    PersonaJuridica persona = (PersonaJuridica) d;
                    persona.setRazonSocial(request.razonSocial());
                    persona.setRubro(request.rubro());
                    persona.setTipo(request.tipo());
                    persona.setCuit(request.cuit());
                    
                    persona = (PersonaJuridica) donanteRepository.save(persona);
                    return new ActualizarPersonaJuridicaResponse(persona.getId(), persona.getRazonSocial(), persona.getCuit());
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el donante Persona Juridica"));
    }

    public boolean eliminar(Long id) {
        if (!donanteRepository.existsById(id)) return false;
        donanteRepository.deleteById(id);
        return true;
    }

    private MedioContacto parseContacto(MedioContactoInfo info) {
        if (info == null || info.tipo() == null) return null;
        switch (info.tipo().toLowerCase()) {
            case "email": return new Email(info.valor());
            case "telefono": return new Telefono(info.valor());
            case "whatsapp": return new WhatsApp(info.valor());
            default: return null;
        }
    }

    private MedioContactoInfo parseContactoInfo(MedioContacto mc) {
        if (mc == null) return null;
        if (mc instanceof Email) return new MedioContactoInfo("email", ((Email) mc).getValor());
        if (mc instanceof Telefono) return new MedioContactoInfo("telefono", ((Telefono) mc).getValor());
        if (mc instanceof WhatsApp) return new MedioContactoInfo("whatsapp", ((WhatsApp) mc).getValor());
        return null;
    }
}
