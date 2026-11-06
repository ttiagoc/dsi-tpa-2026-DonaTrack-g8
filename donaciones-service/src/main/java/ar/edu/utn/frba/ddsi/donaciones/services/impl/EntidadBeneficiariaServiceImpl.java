package ar.edu.utn.frba.ddsi.donaciones.services.impl;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadResponse;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.TipoNecesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.Periodo;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.SubcategoriaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.Coordenadas;
import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.Geocodificador;
import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.GeocodificadorNoDisponibleException;

@Service
public class EntidadBeneficiariaServiceImpl implements EntidadBeneficiariaService {

    private static final Logger LOG = Logger.getLogger(EntidadBeneficiariaServiceImpl.class.getName());

    private final EntidadBeneficiariaRepository entidadBeneficiariaRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final Geocodificador geocodificador;

    public EntidadBeneficiariaServiceImpl(EntidadBeneficiariaRepository entidadBeneficiariaRepository,
            SubcategoriaRepository subcategoriaRepository, Geocodificador geocodificador) {
        this.entidadBeneficiariaRepository = entidadBeneficiariaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
        this.geocodificador = geocodificador;
    }

    /**
     * Verifica que la direccion exista y guarda donde queda. Si la direccion no existe, el alta o la edicion
     * se rechaza (DireccionInvalidaException). Si Georef no responde no se bloquea a la entidad: queda sin
     * ubicar y se reintenta con {@link #ubicarPendientes()}.
     */
    private void ubicar(EntidadBeneficiaria entidad) {
        try {
            Coordenadas coordenadas = geocodificador.ubicar(entidad.getDireccion());
            entidad.ubicarEn(coordenadas.latitud(), coordenadas.longitud());
        } catch (GeocodificadorNoDisponibleException e) {
            LOG.warning("No se pudo verificar la dirección de " + entidad.getRazonSocial() + ": " + e.getMessage());
        }
    }

    @Override
    public int ubicarPendientes() {
        int ubicadas = 0;
        for (EntidadBeneficiaria entidad : entidadBeneficiariaRepository.findAll()) {
            if (entidad.estaUbicada()) {
                continue;
            }
            try {
                Coordenadas coordenadas = geocodificador.ubicar(entidad.getDireccion());
                entidad.ubicarEn(coordenadas.latitud(), coordenadas.longitud());
                this.guardar(entidad);
                ubicadas++;
            } catch (GeocodificadorNoDisponibleException e) {
                LOG.warning("Georef no responde; las entidades sin ubicar se reintentan en el próximo arranque");
                break;
            } catch (BusinessException e) {
                // cargada antes de que se verificaran las direcciones: queda fuera del mapa hasta que la corrijan
                LOG.warning("Entidad " + entidad.getId() + ": " + e.getMessage());
            }
        }
        return ubicadas;
    }

    public List<EntidadBeneficiariaResponse> obtenerTodas() {
        return entidadBeneficiariaRepository.findAll().stream()
                .map(this::toEntidadBeneficiariaResponse)
                .collect(Collectors.toList());
    }

    public EntidadBeneficiariaResponse obtenerPorId(Long id) {
        EntidadBeneficiaria e = entidadBeneficiariaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una entidad beneficiaria con el id: " + id));

        return toEntidadBeneficiariaResponse(e);
    }

    private EntidadBeneficiariaResponse toEntidadBeneficiariaResponse(EntidadBeneficiaria e) {
        List<NecesidadResponse> necesidades = e.getNecesidades() != null ? e.getNecesidades().stream()
                .map(this::toNecesidadResponse).collect(Collectors.toList()) : new java.util.ArrayList<>();

        return new EntidadBeneficiariaResponse(
                e.getId(), e.getRazonSocial(), e.getDireccion(), e.getTelefono().getValor(),
                e.getCorreoRepresentantes().stream().map(correo -> correo.getValor()).collect(Collectors.toList()),
                necesidades);
    }

    private NecesidadResponse toNecesidadResponse(Necesidad n) {
        String periodo = n.getTipoNecesidad() instanceof NecesidadRecurrente recurrente
                ? recurrente.getPeriodo().toString()
                : null;
        String categoria = n.getSubcategoria().getCategoria() != null ? n.getSubcategoria().getCategoria().getNombre() : null;
        return new NecesidadResponse(n.getId(), n.getSubcategoria().getNombre(),
                n.getTipoNecesidad().getClass().getSimpleName(), periodo, n.getDescripcion(), n.getCantidad(),
                categoria, n.cantidadAsignada(), n.estaSatisfecha(), n.donacionesEnCurso(),
                n.getUnidad().name());
    }

    public EntidadBeneficiariaResponse crear(EntidadBeneficiariaRequest request) {
        EntidadBeneficiaria entidad = toEntidadBeneficiaria(request);
        ubicar(entidad);
        return toEntidadBeneficiariaResponse(this.guardar(entidad));
    }

    private EntidadBeneficiaria toEntidadBeneficiaria(EntidadBeneficiariaRequest request) {
        if (request.razonSocial() == null || request.razonSocial().isBlank()) {
            throw new BusinessException("La razon social no puede ser nula ni estar vacia");
        }
        if (request.direccion() == null || request.direccion().isBlank()) {
            throw new BusinessException("La direccion no puede ser nula ni estar vacia");
        }
        if (request.telefono() == null || request.telefono().isBlank()) {
            throw new BusinessException("El telefono no puede ser nulo ni estar vacio");
        }
        if (request.correoRepresentantes() == null || request.correoRepresentantes().isEmpty()) {
            throw new BusinessException("Debe haber al menos un correo de representante");
        }

        return new EntidadBeneficiaria(request.razonSocial(), request.direccion(),
                request.telefono(),
                request.correoRepresentantes().stream().map(this::toMedioContacto).collect(Collectors.toList()));
    }

    private MedioContacto toMedioContacto(MedioContactoRequest request) {
        if (request == null || request.tipo() == null) {
            throw new BusinessException("El medio de contacto y su tipo no pueden ser nulos");
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

    public boolean eliminar(Long id) {
        if (!entidadBeneficiariaRepository.existsById(id))
            return false;
        entidadBeneficiariaRepository.deleteById(id);
        return true;
    }

    public EntidadBeneficiariaResponse actualizar(Long id, EntidadBeneficiariaRequest request) {
        EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una entidad beneficiaria con el id: " + id));
        if (request.razonSocial() != null && !request.razonSocial().isBlank()) {
            entidad.setRazonSocial(request.razonSocial());
        }
        if (request.direccion() != null && !request.direccion().isBlank()
                && !request.direccion().equals(entidad.getDireccion())) {
            entidad.mudarseA(request.direccion());
            ubicar(entidad);
        }
        if (request.telefono() != null && !request.telefono().isBlank()) {
            entidad.setTelefono(new MedioContacto(request.telefono(), TipoContacto.SMS));
        }
        if (request.correoRepresentantes() != null && !request.correoRepresentantes().isEmpty()) {
            entidad.setCorreoRepresentantes(request.correoRepresentantes().stream().map(this::toMedioContacto)
                    .collect(Collectors.toList()));
        }
        return toEntidadBeneficiariaResponse(this.guardar(entidad));
    }

    public List<NecesidadResponse> obtenerNecesidades(Long id) {
        return entidadBeneficiariaRepository.findById(id)
                .map(e -> e.getNecesidades().stream()
                        .map(this::toNecesidadResponse)
                        .collect(Collectors.toList()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una entidad beneficiaria con el id: " + id));
    }

    private Necesidad toNecesidad(NecesidadRequest request) {
        if (request.descripcion() == null || request.descripcion().isBlank()) {
            throw new BusinessException("La descripcion de la necesidad no puede ser nula ni estar vacia");
        }
        if (request.cantidad() == null || request.cantidad() <= 0) {
            throw new BusinessException("La cantidad de la necesidad debe ser mayor a 0");
        }
        return new Necesidad(toSubcategoria(request.subcategoria()),
                toTipoNecesidad(request.tipoNecesidad(), request.periodo()),
                request.descripcion(), request.cantidad(), toUnidad(request.unidad()));
    }

    private UnidadDeMedida toUnidad(String unidad) {
        if (unidad == null || unidad.isBlank()) {
            return UnidadDeMedida.UNIDADES;
        }
        try {
            return UnidadDeMedida.valueOf(unidad.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unidad '" + unidad
                    + "' no valida. Valores posibles: UNIDADES, KILOGRAMOS, LITROS");
        }
    }

    // Las subcategorias se cargan previamente desde el catalogo (CategoriaController):
    // una necesidad solo puede referenciar una subcategoria existente.
    private Subcategoria toSubcategoria(SubcategoriaRequest subcategoria) {
        if (subcategoria == null || subcategoria.nombre() == null || subcategoria.nombre().isBlank()) {
            throw new BusinessException("El nombre de la subcategoria no puede ser nulo ni estar vacio");
        }
        return subcategoriaRepository.findByNombre(subcategoria.nombre())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la subcategoria '" + subcategoria.nombre() + "' en el catalogo"));
    }

    private TipoNecesidad toTipoNecesidad(String tipo, String periodo) {
        if (tipo == null || tipo.isBlank()) {
            throw new BusinessException("El tipo de necesidad no puede ser nulo ni estar vacio");
        }
        switch (tipo.toLowerCase()) {
            case "recurrente":
                return new NecesidadRecurrente(toPeriodo(periodo));
            case "extraordinaria":
                return new NecesidadExtraordinaria();
            default:
                throw new BusinessException("Tipo de necesidad invalido");
        }
    }

    private Periodo toPeriodo(String periodo) {
        if (periodo == null || periodo.isBlank()) {
            throw new BusinessException("Una necesidad recurrente debe indicar su periodo");
        }
        try {
            return Periodo.valueOf(periodo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Periodo '" + periodo
                    + "' no valido. Valores posibles: DIARIO, SEMANAL, MENSUAL, ANUAL");
        }
    }

    public NecesidadResponse registrarNecesidad(Long id, NecesidadRequest request) {
        EntidadBeneficiaria entidadBeneficiaria = entidadBeneficiariaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una entidad beneficiaria con el id: " + id));
        Necesidad necesidad = toNecesidad(request);
        entidadBeneficiaria.registrarNecesidad(necesidad);
        this.guardar(entidadBeneficiaria);
        return toNecesidadResponse(necesidad);
    }

    public void eliminarNecesidad(Long entidadId, Long necesidadId) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    entidad.eliminarNecesidad(necesidadId);
                    return this.guardar(entidad);
                })
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una entidad beneficiaria con el id: " + entidadId));
    }

    private EntidadBeneficiaria guardar(EntidadBeneficiaria entidadBeneficiaria) {
        return entidadBeneficiariaRepository.save(entidadBeneficiaria);
    }

    @Override
    public List<MedioContacto> obtenerContactos(Long id) {
        EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una entidad beneficiaria con el id: " + id));
        return entidad.getCorreoRepresentantes();
    }
}
