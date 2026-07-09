package ar.edu.utn.frba.ddsi.donaciones.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.ReportarNoRecibidaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.SubirFotosRecepcionRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.TipoNecesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import ar.edu.utn.frba.ddsi.donaciones.services.EventoService;

@Service
public class EntidadBeneficiariaServiceImpl implements EntidadBeneficiariaService {

    private final EntidadBeneficiariaRepository entidadBeneficiariaRepository;
    private final DonacionRepository donacionRepository;
    private final EventoService eventoService;

    public EntidadBeneficiariaServiceImpl(EntidadBeneficiariaRepository entidadBeneficiariaRepository,
            DonacionRepository donacionRepository, EventoService eventoService) {
        this.entidadBeneficiariaRepository = entidadBeneficiariaRepository;
        this.donacionRepository = donacionRepository;
        this.eventoService = eventoService;
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
        return new NecesidadResponse(n.getId(), n.getSubcategoria().getNombre(),
                n.getTipoNecesidad().getClass().getSimpleName(), n.getDescripcion(), n.getCantidad());
    }

    public EntidadBeneficiariaResponse crear(EntidadBeneficiariaRequest request) {
        EntidadBeneficiaria entidad = toEntidadBeneficiaria(request);
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
        if (request.direccion() != null && !request.direccion().isBlank()) {
            entidad.setDireccion(request.direccion());
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
        return new Necesidad(toSubcategoria(request.subcategoria()), toTipoNecesidad(request.tipoNecesidad()),
                request.descripcion(), request.cantidad());
    }

    private Subcategoria toSubcategoria(SubcategoriaRequest subcategoria) {
        if (subcategoria.nombre() == null || subcategoria.nombre().isBlank()) {
            throw new BusinessException("El nombre de la subcategoria no puede ser nulo ni estar vacio");
        }
        return new Subcategoria(subcategoria.nombre(), toCategoria(subcategoria.categoria()));
    }

    private Categoria toCategoria(CategoriaRequest categoria) {
        if (categoria.nombre() == null || categoria.nombre().isBlank()) {
            throw new BusinessException("El nombre de la categoria no puede ser nulo ni estar vacio");
        }
        return new Categoria(categoria.nombre(), categoria.pideEstado(), categoria.esPerecedero());
    }

    private TipoNecesidad toTipoNecesidad(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new BusinessException("El tipo de necesidad no puede ser nulo ni estar vacio");
        }
        switch (tipo.toLowerCase()) {
            case "recurrente":
                return new NecesidadRecurrente();
            case "extraordinaria":
                return new NecesidadExtraordinaria();
            default:
                throw new BusinessException("Tipo de necesidad invalido");
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

    public void confirmarEntrega(Long entidadId, Long donacionId) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    Donacion donacion = donacionRepository.findById(donacionId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "No se encontro una donacion con el id: " + donacionId));
                    entidad.confirmarEntrega(donacion);
                    this.guardar(entidad);
                    return entidad;
                })
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una entidad beneficiaria con el id: " + entidadId));
    }

    public void reportarNoRecibida(Long entidadId, Long donacionId, ReportarNoRecibidaRequest request) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    eventoService.notificarEntregaFallida(donacionId, request.motivo());
                    return entidad;
                })
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una entidad beneficiaria con el id: " + entidadId));
    }

    public void subirFotosRecepcion(Long entidadId, Long donacionId, SubirFotosRecepcionRequest request) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    Donacion donacion = donacionRepository.findById(donacionId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "No se encontro una donacion con el id: " + donacionId));
                    if (donacion.getFotosRecepcion() == null) {
                        donacion.setFotosRecepcion(new java.util.ArrayList<>());
                    }
                    if (request.fotosUrl() != null) {
                        donacion.getFotosRecepcion().addAll(request.fotosUrl());
                    }
                    donacionRepository.save(donacion);
                    return entidad;
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
