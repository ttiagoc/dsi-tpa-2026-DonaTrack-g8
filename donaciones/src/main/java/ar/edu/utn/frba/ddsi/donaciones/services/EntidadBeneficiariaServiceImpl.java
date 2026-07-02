package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Telefono;
import ar.edu.utn.frba.ddsi.common.models.entities.WhatsApp;
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
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));

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
        return toEntidadBeneficiariaResponse(entidadBeneficiariaRepository.save(entidad));
    }

    private EntidadBeneficiaria toEntidadBeneficiaria(EntidadBeneficiariaRequest request) {
        return new EntidadBeneficiaria(request.razonSocial(), request.direccion(),
                request.telefono(),
                request.correoRepresentantes().stream().map(this::toMedioContacto).collect(Collectors.toList()));
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

    public boolean eliminar(Long id) {
        if (!entidadBeneficiariaRepository.existsById(id))
            return false;
        entidadBeneficiariaRepository.deleteById(id);
        return true;
    }

    public EntidadBeneficiariaResponse actualizar(Long id, EntidadBeneficiariaRequest request) {
        EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
        entidad.setRazonSocial(request.razonSocial());
        entidad.setDireccion(request.direccion());
        entidad.setTelefono(new MedioContacto(request.telefono(), new Telefono()));
        entidad.setCorreoRepresentantes(request.correoRepresentantes().stream().map(this::toMedioContacto)
                .collect(Collectors.toList()));
        return toEntidadBeneficiariaResponse(entidadBeneficiariaRepository.save(entidad));
    }

    public List<NecesidadResponse> obtenerNecesidades(Long id) {
        return entidadBeneficiariaRepository.findById(id)
                .map(e -> e.getNecesidades().stream()
                        .map(this::toNecesidadResponse)
                        .collect(Collectors.toList()))
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    private Necesidad toNecesidad(NecesidadRequest request) {
        return new Necesidad(toSubcategoria(request.subcategoria()), toTipoNecesidad(request.tipoNecesidad()),
                request.descripcion(), request.cantidad());
    }

    private Subcategoria toSubcategoria(SubcategoriaRequest subcategoria) {
        return new Subcategoria(subcategoria.nombre(), toCategoria(subcategoria.categoria()));
    }

    private Categoria toCategoria(CategoriaRequest categoria) {
        return new Categoria(categoria.nombre(), categoria.pideEstado(), categoria.esPerecedero());
    }

    private TipoNecesidad toTipoNecesidad(String tipo) {
        switch (tipo.toLowerCase()) {
            case "recurrente":
                return new NecesidadRecurrente();
            case "extraordinaria":
                return new NecesidadExtraordinaria();
            default:
                throw new IllegalArgumentException("Tipo de necesidad invalido");
        }
    }

    public NecesidadResponse registrarNecesidad(Long id, NecesidadRequest request) {
        EntidadBeneficiaria entidadBeneficiaria = entidadBeneficiariaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
        Necesidad necesidad = toNecesidad(request);
        entidadBeneficiaria.registrarNecesidad(necesidad);
        entidadBeneficiariaRepository.save(entidadBeneficiaria);
        return toNecesidadResponse(necesidad);
    }

    public void eliminarNecesidad(Long entidadId, Long necesidadId) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    entidad.eliminarNecesidad(necesidadId);
                    return entidadBeneficiariaRepository.save(entidad);
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    public void confirmarEntrega(Long entidadId, Long donacionId) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    Donacion donacion = donacionRepository.findById(donacionId)
                            .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
                    entidad.confirmarEntrega(donacion);
                    entidadBeneficiariaRepository.save(entidad);
                    return entidad;
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    public void reportarNoRecibida(Long entidadId, Long donacionId, ReportarNoRecibidaRequest request) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    eventoService.notificarEntregaFallida(donacionId, request.motivo());
                    return entidad;
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    public void subirFotosRecepcion(Long entidadId, Long donacionId, SubirFotosRecepcionRequest request) {
        entidadBeneficiariaRepository.findById(entidadId)
                .map(entidad -> {
                    Donacion donacion = donacionRepository.findById(donacionId)
                            .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
                    if (donacion.getFotosRecepcion() == null) {
                        donacion.setFotosRecepcion(new java.util.ArrayList<>());
                    }
                    if (request.fotosUrl() != null) {
                        donacion.getFotosRecepcion().addAll(request.fotosUrl());
                    }
                    donacionRepository.save(donacion);
                    return entidad;
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }
}
