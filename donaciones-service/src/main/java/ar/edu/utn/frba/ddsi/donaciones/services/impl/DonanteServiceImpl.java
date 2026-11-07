package ar.edu.utn.frba.ddsi.donaciones.services.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.dtos.pagina.PaginaResponse;
import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.DonanteDetalleResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.DonanteResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.RepresentanteRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoOrganizacion;
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

    @Override
    public PaginaResponse<DonanteResponse> obtenerTodosPaginado(int page, int size) {
        PaginaResponse<Donante> pag = donanteRepository.findAllPaginado(page, size);
        List<DonanteResponse> elementos = pag.elementos().stream().map(this::toDonanteResponse).toList();
        return new PaginaResponse<>(elementos, pag.pagina(), pag.totalPaginas(), pag.totalElementos(), pag.porPagina());
    }

    private String getStringTipoDonante(Donante d) {
        if (d instanceof PersonaHumana) {
            return "Persona Humana";
        }
        return "Persona Jurídica";
    }

    private DonanteResponse toDonanteResponse(Donante d) {
        String nombre = null;
        String documento = null;
        if (d instanceof PersonaHumana ph) {
            nombre = ph.getNombre() + (ph.getApellido() != null && !ph.getApellido().isBlank() ? " " + ph.getApellido() : "");
            documento = ph.getDni();
        } else if (d instanceof PersonaJuridica pj) {
            nombre = pj.getRazonSocial();
            documento = pj.getCuit();
        }
        String contacto = (d.getContactoPredeterminado() != null && d.getContactoPredeterminado().getValor() != null)
                ? d.getContactoPredeterminado().getValor()
                : null;
        return new DonanteResponse(
                d.getId(),
                getStringTipoDonante(d),
                nombre,
                documento,
                contacto);
    }

    public DonanteResponse obtenerPorId(Long id) {
        Donante d = donanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro un donante con el id: " + id));
        return toDonanteResponse(d);
    }

    public DonanteDetalleResponse obtenerDetalle(Long id) {
        Donante d = donanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro un donante con el id: " + id));
        List<MedioContactoRequest> contactos = d.getContactos().stream().map(this::toMedioContactoRequest).toList();
        MedioContactoRequest predeterminado = d.getContactoPredeterminado() != null
                ? toMedioContactoRequest(d.getContactoPredeterminado())
                : null;
        // cada entrega (RegistroDonacion) se segmenta en una o mas donaciones: se cuentan las donaciones
        int cantidadDonaciones = d.getDonaciones().stream().mapToInt(r -> r.getDonaciones().size()).sum();
        LocalDate ultimaDonacion = d.getDonaciones().stream()
                .map(RegistroDonacion::getFecha)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .map(LocalDateTime::toLocalDate)
                .orElse(null);

        if (d instanceof PersonaHumana ph) {
            return new DonanteDetalleResponse(d.getId(), "HUMANA", ph.getNombre(), ph.getApellido(), ph.getDni(),
                    ph.getFechaNacimiento(), ph.getGenero(), ph.getDireccion(), null, null, null, null, List.of(),
                    contactos, predeterminado, cantidadDonaciones, ultimaDonacion);
        }
        PersonaJuridica pj = (PersonaJuridica) d;
        List<RepresentanteRequest> representantes = pj.getRepresentantes() == null ? List.of()
                : pj.getRepresentantes().stream()
                        .map(r -> new RepresentanteRequest(r.getNombre(), r.getApellido(),
                                r.getCorreo() != null ? r.getCorreo().getValor() : null))
                        .toList();
        return new DonanteDetalleResponse(d.getId(), "JURIDICA", null, null, null, null, null, null,
                pj.getRazonSocial(), pj.getRubro(), pj.getTipo() != null ? pj.getTipo().name() : null, pj.getCuit(),
                representantes, contactos, predeterminado, cantidadDonaciones, ultimaDonacion);
    }

    private MedioContactoRequest toMedioContactoRequest(MedioContacto contacto) {
        return new MedioContactoRequest(
                contacto.getTipoContacto() != null ? contacto.getTipoContacto().name() : null, contacto.getValor());
    }

    // Nombre, DNI y medios de contacto los valida el constructor de PersonaHumana (valen también para el CSV).
    // Acá se exigen los datos completos de registro que la importación masiva no trae.
    private PersonaHumana toPersonaHumana(PersonaHumanaRequest request) {
        if (request.apellido() == null || request.apellido().isBlank()) {
            throw new BusinessException("El apellido de la persona humana no puede ser nulo ni estar vacio");
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

        return new PersonaHumana(request.contactos().stream().map(this::toMedioContacto)
                .collect(Collectors.toList()), toMedioContacto(request.contactoPredeterminado()), request.nombre(),
                request.apellido(), request.fechaNacimiento(), request.dni(), request.genero(),
                request.direccion());
    }

    // Razón social, CUIT y medios de contacto los valida el constructor de PersonaJuridica.
    private PersonaJuridica toPersonaJuridica(PersonaJuridicaRequest request) {
        if (request.rubro() == null || request.rubro().isBlank()) {
            throw new BusinessException("El rubro de la persona juridica no puede ser nulo ni estar vacio");
        }
        if (request.tipo() == null || request.tipo().isBlank()) {
            throw new BusinessException("El tipo de la persona juridica no puede ser nulo ni estar vacio");
        }
        if (request.representantes() == null || request.representantes().isEmpty()) {
            throw new BusinessException("Debe haber al menos un representante");
        }
        if (request.contactos() == null || request.contactos().isEmpty()) {
            throw new BusinessException("Debe haber al menos un medio de contacto");
        }

        return new PersonaJuridica(
                request.contactos().stream().map(this::toMedioContacto).collect(Collectors.toList()),
                toMedioContacto(request.contactoPredeterminado()), request.razonSocial(), request.rubro(),
                toTipoOrganizacion(request.tipo()), request.cuit(),
                request.representantes().stream().map(this::toRepresentante).collect(Collectors.toList()));
    }

    private TipoOrganizacion toTipoOrganizacion(String tipo) {
        try {
            return TipoOrganizacion.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tipo de organizacion '" + tipo
                    + "' no valido. Valores posibles: GUBERNAMENTAL, ONG, EMPRESA, INSTITUCION");
        }
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
        PersonaHumana persona = (PersonaHumana) donanteRepository.save(toPersonaHumana(request));
        return toDonanteResponse(persona);
    }

    public DonanteResponse crearPersonaJuridica(PersonaJuridicaRequest request) {
        PersonaJuridica persona = (PersonaJuridica) donanteRepository.save(toPersonaJuridica(request));
        return toDonanteResponse(persona);
    }

    public DonanteResponse actualizarPersonaHumana(Long id, PersonaHumanaRequest request) {
        PersonaHumana d = donanteRepository.findById(id)
                .filter(PersonaHumana.class::isInstance)
                .map(PersonaHumana.class::cast)
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
        actualizarContactos(d, request.contactos(), request.contactoPredeterminado());

        return toDonanteResponse(donanteRepository.save(d));
    }

    public DonanteResponse actualizarPersonaJuridica(Long id, PersonaJuridicaRequest request) {
        PersonaJuridica d = donanteRepository.findById(id)
                .filter(PersonaJuridica.class::isInstance)
                .map(PersonaJuridica.class::cast)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro un donante de tipo Persona Juridica con id: " + id));

        if (request.razonSocial() != null && !request.razonSocial().isBlank()) {
            d.setRazonSocial(request.razonSocial());
        }
        if (request.rubro() != null && !request.rubro().isBlank()) {
            d.setRubro(request.rubro());
        }
        if (request.tipo() != null && !request.tipo().isBlank()) {
            d.setTipo(toTipoOrganizacion(request.tipo()));
        }
        if (request.representantes() != null && !request.representantes().isEmpty()) {
            d.setRepresentantes(
                    request.representantes().stream().map(this::toRepresentante).collect(Collectors.toList()));
        }
        actualizarContactos(d, request.contactos(), request.contactoPredeterminado());

        return toDonanteResponse(donanteRepository.save(d));
    }

    // Los campos que no vienen en el request conservan su valor; la entidad valida la combinación resultante.
    private void actualizarContactos(Donante d, List<MedioContactoRequest> contactos,
            MedioContactoRequest contactoPredeterminado) {
        List<MedioContacto> nuevosContactos = contactos != null && !contactos.isEmpty()
                ? contactos.stream().map(this::toMedioContacto).collect(Collectors.toList())
                : d.getContactos();
        MedioContacto nuevoPredeterminado = contactoPredeterminado != null
                ? toMedioContacto(contactoPredeterminado)
                : d.getContactoPredeterminado();
        d.actualizarContactos(nuevosContactos, nuevoPredeterminado);
    }

    public boolean eliminar(Long id) {
        Optional<Donante> donante = donanteRepository.findById(id);
        if (donante.isEmpty()) {
            return false;
        }
        // sus registros de donacion lo referencian y se conservan por trazabilidad
        if (!donante.get().getDonaciones().isEmpty()) {
            throw new BusinessException("No se puede eliminar un donante con donaciones registradas");
        }
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
            case "sms":
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
