package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.ActualizarEntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.ActualizarEntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.CrearEntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.CrearEntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadInfo;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.ObtenerEntidadResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.ObtenerNecesidadesResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.ObtenerTodasEntidadesResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.RegistrarNecesidadRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.RegistrarNecesidadResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.ReportarNoRecibidaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.SubirFotosRecepcionRequest;
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
public class EntidadBeneficiariaService {

    private final EntidadBeneficiariaRepository entidadBeneficiariaRepository;
    private final DonacionRepository donacionRepository;
    private final EventoService eventoService;

    public EntidadBeneficiariaService(EntidadBeneficiariaRepository entidadBeneficiariaRepository,
            DonacionRepository donacionRepository, EventoService eventoService) {
        this.entidadBeneficiariaRepository = entidadBeneficiariaRepository;
        this.donacionRepository = donacionRepository;
        this.eventoService = eventoService;
    }

    public List<ObtenerTodasEntidadesResponse> obtenerTodas() {
        return entidadBeneficiariaRepository.findAll().stream()
                .map(e -> new ObtenerTodasEntidadesResponse(e.getId(), e.getRazonSocial(), e.getDireccion()))
                .collect(Collectors.toList());
    }

    public ObtenerEntidadResponse obtenerPorId(Long id) {
        EntidadBeneficiaria e = entidadBeneficiariaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));

        List<NecesidadInfo> necesidades = e.getNecesidades() != null ? e.getNecesidades().stream()
                .map(this::mapNecesidadToInfo).collect(Collectors.toList()) : new java.util.ArrayList<>();

        return new ObtenerEntidadResponse(
                e.getId(), e.getRazonSocial(), e.getDireccion(), e.getTelefono().toString(),
                e.getCorreoRepresentantes().stream().map(correo -> correo.toString()).collect(Collectors.toList()),
                necesidades);
    }

    public CrearEntidadBeneficiariaResponse crear(CrearEntidadBeneficiariaRequest request) {
        EntidadBeneficiaria entidad = new EntidadBeneficiaria();
        entidad.setRazonSocial(request.razonSocial());
        entidad.setDireccion(request.direccion());
        Telefono telefono = new Telefono();
        telefono.setValor(request.telefono().toString());
        entidad.setTelefono(telefono);
        List<Email> correos = request.correoRepresentantes().stream().map(correo -> {
            Email c = new Email();
            c.setValor(correo.toString());
            return c;
        }).collect(Collectors.toList());
        entidad.setCorreoRepresentantes(correos);
        entidad = entidadBeneficiariaRepository.save(entidad);
        return new CrearEntidadBeneficiariaResponse(entidad.getId(), entidad.getRazonSocial());
    }

    public boolean eliminar(Long id) {
        if (!entidadBeneficiariaRepository.existsById(id))
            return false;
        entidadBeneficiariaRepository.deleteById(id);
        return true;
    }

    public ActualizarEntidadBeneficiariaResponse actualizar(Long id, ActualizarEntidadBeneficiariaRequest request) {
        return entidadBeneficiariaRepository.findById(id)
                .map(e -> {
                    e.setRazonSocial(request.razonSocial());
                    e.setDireccion(request.direccion());
                    Telefono telefono = new Telefono();
                    telefono.setValor(request.telefono().toString());
                    e.setTelefono(telefono);
                    List<Email> correos = request.correoRepresentantes().stream().map(correo -> {
                        Email c = new Email();
                        c.setValor(correo.toString());
                        return c;
                    }).collect(Collectors.toList());
                    e.setCorreoRepresentantes(correos);
                    e = entidadBeneficiariaRepository.save(e);
                    return new ActualizarEntidadBeneficiariaResponse(e.getId(), e.getRazonSocial(), e.getDireccion());
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    public ObtenerNecesidadesResponse obtenerNecesidades(Long id) {
        return entidadBeneficiariaRepository.findById(id)
                .map(e -> {
                    List<NecesidadInfo> necesidades = e.getNecesidades() != null ? e.getNecesidades().stream()
                            .map(this::mapNecesidadToInfo).collect(Collectors.toList()) : new java.util.ArrayList<>();
                    return new ObtenerNecesidadesResponse(necesidades);
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
    }

    public RegistrarNecesidadResponse registrarNecesidad(Long id, RegistrarNecesidadRequest request) {
        return entidadBeneficiariaRepository.findById(id)
                .map(e -> {
                    Necesidad necesidad = new Necesidad();
                    if (request.subcategoria() != null) {
                        Subcategoria sub = new Subcategoria();
                        sub.setNombre(request.subcategoria());
                        necesidad.setSubcategoria(sub);
                    }
                    necesidad.setDescripcion(request.descripcion());
                    necesidad.setCantidad(request.cantidad());
                    necesidad.setTipoNecesidad(parseTipoNecesidad(request.tipoNecesidad()));

                    e.registrarNecesidad(necesidad);
                    e = entidadBeneficiariaRepository.save(e);

                    Necesidad nGuardada = e.getNecesidades().get(e.getNecesidades().size() - 1);
                    return new RegistrarNecesidadResponse(nGuardada.getId(), nGuardada.getDescripcion(),
                            nGuardada.getCantidad(), request.tipoNecesidad());
                })
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entidad beneficiaria"));
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

    private NecesidadInfo mapNecesidadToInfo(Necesidad n) {
        String tipo = n.getTipoNecesidad() != null ? n.getTipoNecesidad().getClass().getSimpleName() : null;
        String sub = n.getSubcategoria() != null ? n.getSubcategoria().getNombre() : null;
        return new NecesidadInfo(n.getId(), sub, tipo, n.getDescripcion(), n.getCantidad());
    }

    private TipoNecesidad parseTipoNecesidad(String tipo) {
        if (tipo == null)
            return null;
        if (tipo.toLowerCase().contains("recurrente"))
            return new NecesidadRecurrente();
        if (tipo.toLowerCase().contains("extraordinaria"))
            return new NecesidadExtraordinaria();
        return null;
    }
}
