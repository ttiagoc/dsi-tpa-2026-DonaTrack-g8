package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Telefono;
import ar.edu.utn.frba.ddsi.common.models.entities.WhatsApp;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.ActualizarPersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.ActualizarPersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.CrearPersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.DonanteResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaHumanaRequest;
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

    public List<DonanteResponse> obtenerTodos() {
        return donanteRepository.findAll().stream()
                .map(this::toDonanteResponse)
                .collect(Collectors.toList());
    }

    private String getStringTipoDonante(Donante d) {
        if (d instanceof PersonaHumana) {
            return "Persona Humana";
        }
        return "Persona Jurídica";
    }

    private DonanteResponse toDonanteResponse(Donante d) {
        return new DonanteResponse(
                d.getId(),
                getStringTipoDonante(d),
                d.getContactoPredeterminado().getValor());
    }

    public DonanteResponse obtenerPorId(Long id) {
        Donante d = donanteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el donante"));
        return toDonanteResponse(d);
    }

    private PersonaHumana toPersonaHumana(PersonaHumanaRequest request) {
        PersonaHumana persona = new PersonaHumana();
        persona.setNombre(request.nombre());
        persona.setApellido(request.apellido());
        persona.setFechaNacimiento(request.fechaNacimiento());
        persona.setDni(request.dni());
        persona.setGenero(request.genero());
        persona.setDireccion(request.direccion());
        if (request.contactos() != null) {
            persona.setContactos(request.contactos().stream().map(this::toMedioContacto).collect(Collectors.toList()));
        }
        return persona;
    }

    private PersonaJuridica toPersonaJuridica(CrearPersonaJuridicaRequest request) {
        PersonaJuridica persona = new PersonaJuridica();
        persona.setRazonSocial(request.razonSocial());
        persona.setRubro(request.rubro());
        persona.setTipo(request.tipo());
        persona.setCuit(request.cuit());

        if (request.representantes() != null) {
            List<Representante> representantes = request.representantes().stream()
                    .map(r -> new Representante(r.nombre(), r.apellido(), new MedioContacto(r.correo(), new Email())))
                    .collect(Collectors.toList());
            persona.setRepresentantes(representantes);
        }
        return persona;
    }

    public DonanteResponse crearPersonaHumana(PersonaHumanaRequest request) {
        if (request.contactos().stream().noneMatch(this::esEmail)) {
            throw new IllegalArgumentException("Debe haber al menos un medio de contacto de tipo Email");
        }
        PersonaHumana persona = (PersonaHumana) donanteRepository.save(toPersonaHumana(request));
        return toDonanteResponse(persona);
    }

    private Boolean esEmail(MedioContactoRequest contacto) {
        return contacto.tipo().equalsIgnoreCase("email");
    }

    public DonanteResponse crearPersonaJuridica(CrearPersonaJuridicaRequest request) {
        PersonaJuridica persona = (PersonaJuridica) donanteRepository.save(toPersonaJuridica(request));
        return toDonanteResponse(persona);
    }

    public DonanteResponse actualizarPersonaHumana(Long id, ActualizarPersonaHumanaRequest request) {
        return donanteRepository.findById(id)
                .filter(d -> d instanceof PersonaHumana)
                .map(d -> toDonanteResponse(donanteRepository.save(toPersonaHumanaActualizar(request))))
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el donante Persona Humana"));
    }

    private PersonaHumana toPersonaHumanaActualizar(ActualizarPersonaHumanaRequest request) {
        PersonaHumana persona = new PersonaHumana();
        persona.setNombre(request.nombre());
        persona.setApellido(request.apellido());
        persona.setFechaNacimiento(request.fechaNacimiento());
        persona.setDni(request.dni());
        persona.setGenero(request.genero());
        persona.setDireccion(request.direccion());
        return persona;
    }

    private PersonaJuridica toPersonaJuridicaActualizar(ActualizarPersonaJuridicaRequest request) {
        PersonaJuridica persona = new PersonaJuridica();
        persona.setRazonSocial(request.razonSocial());
        persona.setRubro(request.rubro());
        persona.setTipo(request.tipo());
        persona.setCuit(request.cuit());
        return persona;
    }

    public DonanteResponse actualizarPersonaJuridica(Long id,
            ActualizarPersonaJuridicaRequest request) {
        return donanteRepository.findById(id)
                .filter(d -> d instanceof PersonaJuridica)
                .map(d -> toDonanteResponse(donanteRepository.save(toPersonaJuridicaActualizar(request))))
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el donante Persona Juridica"));
    }

    public boolean eliminar(Long id) {
        if (!donanteRepository.existsById(id))
            return false;
        donanteRepository.deleteById(id);
        return true;
    }

    private MedioContacto toMedioContacto(MedioContactoRequest request) {
        if (request == null || request.tipo() == null) {
            return null;
        }
        MedioContacto contacto = new MedioContacto();
        contacto.setValor(request.valor());
        switch (request.tipo().toLowerCase()) {
            case "email":
                contacto.setEstrategia(new Email());
                break;
            case "telefono":
                contacto.setEstrategia(new Telefono());
                break;
            case "whatsapp":
                contacto.setEstrategia(new WhatsApp());
                break;
            default:
                return null;
        }
        return contacto;
    }
}
