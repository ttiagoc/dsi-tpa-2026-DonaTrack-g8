package ar.edu.utn.frba.ddsi.donaciones.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.DonanteResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.RepresentanteRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;

@Service
public class DonanteServiceImpl implements DonanteService {

    private final DonanteRepository donanteRepository;

    public DonanteServiceImpl(DonanteRepository donanteRepository) {
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
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro un donante con el id: " + id));
        return toDonanteResponse(d);
    }

    private PersonaHumana toPersonaHumana(PersonaHumanaRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre de la persona humana no puede ser nulo ni estar vacio");
        }
        if (request.apellido() == null || request.apellido().isBlank()) {
            throw new BusinessException("El apellido de la persona humana no puede ser nulo ni estar vacio");
        }
        if (request.dni() == null || request.dni().isBlank()) {
            throw new BusinessException("El DNI de la persona humana no puede ser nulo ni estar vacio");
        }
        if (request.fechaNacimiento() == null) {
            throw new BusinessException("La fecha de nacimiento de la persona humana no puede ser nula");
        }
        if (request.genero() == null || request.genero().isBlank()) {
            throw new BusinessException("El genero de la persona humana no puede ser nulo ni estar vacio");
        }
        if (request.direccion() == null || request.direccion().isBlank()) {
            throw new BusinessException("La direccion de la persona humana no puede ser nula ni estar vacia");
        }
        if (request.contactos() == null || request.contactos().isEmpty()) {
            throw new BusinessException("Debe haber al menos un medio de contacto");
        }

        return new PersonaHumana(null, request.contactos().stream().map(this::toMedioContacto)
                .collect(Collectors.toList()), toMedioContacto(request.contactoPredeterminado()), request.nombre(),
                request.apellido(), request.fechaNacimiento(), request.dni(), request.genero(),
                request.direccion());
    }

    private PersonaJuridica toPersonaJuridica(PersonaJuridicaRequest request) {
        if (request.razonSocial() == null || request.razonSocial().isBlank()) {
            throw new BusinessException("La razon social de la persona juridica no puede ser nula ni estar vacia");
        }
        if (request.rubro() == null || request.rubro().isBlank()) {
            throw new BusinessException("El rubro de la persona juridica no puede ser nulo ni estar vacio");
        }
        if (request.tipo() == null || request.tipo().isBlank()) {
            throw new BusinessException("El tipo de la persona juridica no puede ser nulo ni estar vacio");
        }
        if (request.cuit() == null || request.cuit().isBlank()) {
            throw new BusinessException("El CUIT de la persona juridica no puede ser nulo ni estar vacio");
        }
        if (request.representantes() == null || request.representantes().isEmpty()) {
            throw new BusinessException("Debe haber al menos un representante");
        }

        return new PersonaJuridica(null,
                request.contactos().stream().map(this::toMedioContacto).collect(Collectors.toList()),
                toMedioContacto(request.contactoPredeterminado()), request.razonSocial(), request.rubro(),
                request.tipo(), request.cuit(),
                request.representantes().stream().map(this::toRepresentante).collect(Collectors.toList()));
    }

    private Representante toRepresentante(RepresentanteRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre del representante no puede ser nulo ni estar vacio");
        }
        if (request.apellido() == null || request.apellido().isBlank()) {
            throw new BusinessException("El apellido del representante no puede ser nulo ni estar vacio");
        }
        if (request.correo() == null || request.correo().isBlank()) {
            throw new BusinessException("El correo del representante no puede ser nulo ni estar vacio");
        }
        return new Representante(request.nombre(), request.apellido(),
                new MedioContacto(request.correo(), TipoContacto.EMAIL));
    }

    public DonanteResponse crearPersonaHumana(PersonaHumanaRequest request) {
        if (request.contactos().stream().noneMatch(this::esEmail)) {
            throw new BusinessException("Debe haber al menos un medio de contacto de tipo Email");
        }
        PersonaHumana persona = (PersonaHumana) donanteRepository.save(toPersonaHumana(request));
        return toDonanteResponse(persona);
    }

    private Boolean esEmail(MedioContactoRequest contacto) {
        return contacto.tipo().equalsIgnoreCase("email");
    }

    public DonanteResponse crearPersonaJuridica(PersonaJuridicaRequest request) {
        PersonaJuridica persona = (PersonaJuridica) donanteRepository.save(toPersonaJuridica(request));
        return toDonanteResponse(persona);
    }

    public DonanteResponse actualizarPersonaHumana(Long id, PersonaHumanaRequest request) {
        PersonaHumana d = (PersonaHumana) donanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro un donante de tipo Persona Humana con id: " + id));

        if (request.nombre() != null && !request.nombre().isBlank()) {
            d.setNombre(request.nombre());
        }
        if (request.apellido() != null && !request.apellido().isBlank()) {
            d.setApellido(request.apellido());
        }
        if (request.fechaNacimiento() != null) {
            d.setFechaNacimiento(request.fechaNacimiento());
        }
        if (request.genero() != null && !request.genero().isBlank()) {
            d.setGenero(request.genero());
        }
        if (request.direccion() != null && !request.direccion().isBlank()) {
            d.setDireccion(request.direccion());
        }
        if (request.contactos() != null && !request.contactos().isEmpty()) {
            d.setContactos(request.contactos().stream().map(this::toMedioContacto).collect(Collectors.toList()));
        }
        if (request.contactoPredeterminado() != null) {
            d.setContactoPredeterminado(toMedioContacto(request.contactoPredeterminado()));
        }

        return toDonanteResponse(donanteRepository.save(d));
    }

    public DonanteResponse actualizarPersonaJuridica(Long id, PersonaJuridicaRequest request) {
        return donanteRepository.findById(id)
                .filter(d -> d instanceof PersonaJuridica)
                .map(d -> toDonanteResponse(donanteRepository.save(toPersonaJuridica(request))))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro un donante de tipo Persona Juridica con id: " + id));
    }

    public boolean eliminar(Long id) {
        if (!donanteRepository.existsById(id))
            return false;
        donanteRepository.deleteById(id);
        return true;
    }

    private MedioContacto toMedioContacto(MedioContactoRequest request) {
        if (request == null || request.tipo() == null) {
            throw new BusinessException("El medio de contacto no puede ser nulo");
        }
        if (request.valor() == null || request.valor().isBlank()) {
            throw new BusinessException("El valor del medio de contacto no puede ser nulo ni estar vacio");
        }
        MedioContacto contacto = new MedioContacto();
        contacto.setValor(request.valor());
        switch (request.tipo().toLowerCase()) {
            case "email":
                contacto.setTipoContacto(TipoContacto.EMAIL);
                break;
            case "telefono":
                contacto.setTipoContacto(TipoContacto.SMS);
                break;
            case "whatsapp":
                contacto.setTipoContacto(TipoContacto.WHATSAPP);
                break;
            default:
                throw new BusinessException("El tipo de medio de contacto no es valido");
        }
        return contacto;
    }

    @Override
    public MedioContacto obtenerContactoPredeterminado(Long id) {
        Donante donante = donanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro un donante con el id: " + id));
        return donante.getContactoPredeterminado();
    }
}
