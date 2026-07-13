package ar.edu.utn.frba.ddsi.donaciones.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.BienRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.BienResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionAsignadaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.SubirFotosRecepcionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.GestorDeEventos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.SegmentadorDeDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;

@Service
public class DonacionServiceImpl implements DonacionService {

    private final DonacionRepository donacionRepository;
    private final SegmentadorDeDonacion segmentadorDeDonacion;
    private final GestorDeEventos gestorDeEventos;
    private final EntidadBeneficiariaRepository entidadBeneficiariaRepository;

    public DonacionServiceImpl(DonacionRepository donacionRepository, SegmentadorDeDonacion segmentadorDeDonacion, GestorDeEventos gestorDeEventos, EntidadBeneficiariaRepository entidadBeneficiariaRepository) {
        this.donacionRepository = donacionRepository;
        this.segmentadorDeDonacion = segmentadorDeDonacion;
        this.gestorDeEventos = gestorDeEventos;
        this.entidadBeneficiariaRepository = entidadBeneficiariaRepository;
    }

    public List<DonacionResponse> obtenerTodas() {
        return donacionRepository.findAll().stream()
                .map(this::toDonacionResponse)
                .collect(Collectors.toList());
    }

    public DonacionResponse obtenerPorId(Long id) {
        Donacion d = donacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una donacion con el id: " + id));

        return this.toDonacionResponse(d);
    }

    public List<DonacionResponse> crear(DonacionRequest request) {
        RegistroDonacion registro = toRegistroDonacion(request);
        List<Donacion> donacionesCreadas = segmentadorDeDonacion.segmentarDonacion(registro);
        donacionesCreadas = donacionRepository.saveAll(donacionesCreadas);

        List<DonacionResponse> donacionesResponses = donacionesCreadas.stream()
                .map(d -> this.toDonacionResponse(d))
                .collect(Collectors.toList());

        return donacionesResponses;
    }

    public boolean eliminar(Long id) {
        return donacionRepository.deleteById(id);
    }

    public EstadoDonacionResponse cambiarEstado(Long id, EstadoDonacionRequest request) {
        Donacion donacion = donacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una donacion con el id: " + id));

        TipoEstadoDonacion nuevoEstado = toTipoEstadoDonacion(request.estado());
        if (nuevoEstado == TipoEstadoDonacion.ENTREGA_FALLIDA) {
            gestorDeEventos.notificarEntregaFallida(id, request.justificacion());
            donacion = donacionRepository.findById(id).orElseThrow();
        } else {
            donacion.cambiarEstado(nuevoEstado, request.justificacion());
            donacion = this.guardar(donacion);
        }

        return new EstadoDonacionResponse(
                donacion.getId(),
                donacion.estadoActual().toString(),
                donacion.getHistorialEstados().get(donacion.getHistorialEstados().size() - 1).getFecha());
    }

    public List<DonacionAsignadaResponse> obtenerDonacionesSegunEstado(String estado, int limit) {
        TipoEstadoDonacion tipoEstado = toTipoEstadoDonacion(estado);

        List<Donacion> donaciones = donacionRepository.buscarPorEstado(tipoEstado);
        if (donaciones.size() > limit) {
            donaciones = donaciones.subList(0, limit);
        }

        List<DonacionAsignadaResponse> donacionesAsignadas = new ArrayList<>();
        for (Donacion donacion : donaciones) {
            donacionesAsignadas.add(new DonacionAsignadaResponse(
                    donacion.getId(),
                    donacion.calcularPesoTotal(),
                    donacion.calcularVolumenTotal(),
                    donacion.obtenerDireccion()));
        }

        return donacionesAsignadas;
    }

    public void subirFotosRecepcion(Long id, SubirFotosRecepcionRequest request) {
        Donacion donacion = donacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una donacion con el id: " + id));
        if (donacion.getFotosRecepcion() == null) {
            donacion.setFotosRecepcion(new java.util.ArrayList<>());
        }
        if (request.fotosUrl() != null) {
            donacion.getFotosRecepcion().addAll(request.fotosUrl());
        }
        donacionRepository.save(donacion);
    }

    public void confirmarEntregaExitosa(ConfirmacionEntregaExitosaRequest request) {
        EntidadBeneficiaria entidad = entidadBeneficiariaRepository.findById(request.entidadId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una entidad beneficiaria con el id: " + request.entidadId()));

        List<Donacion> donaciones = new java.util.ArrayList<>();

        for (Long donacionId : request.donacionIds()) {
            Donacion donacion = donacionRepository.findById(donacionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontro una donacion con el id: " + donacionId));

            entidad.confirmarEntrega(donacion);
            donacionRepository.save(donacion);
            donaciones.add(donacion);
        }

        gestorDeEventos.emitirEntregaExitosa(entidad, donaciones, request.patenteCamion(), request.fechaHora());
    }


    private Donacion guardar(Donacion donacion) {
        return donacionRepository.save(donacion);
    }

    private RegistroDonacion toRegistroDonacion(DonacionRequest donacionRequest) {
        if (donacionRequest.bienes() == null || donacionRequest.bienes().isEmpty()) {
            throw new BusinessException("La donacion debe tener al menos un bien");
        }
        return new RegistroDonacion(donacionRequest.descripcion(),
                donacionRequest.bienes().stream().map(this::toBien).collect(Collectors.toList()));
    }

    private DonacionResponse toDonacionResponse(Donacion d) {
        return new DonacionResponse(
                d.getId(),
                d.getBienes().stream().map(this::toBienResonse).collect(Collectors.toList()),
                d.estadoActual().toString(),
                d.getFecha(),
                d.getDonante().getId(),
                d.getEntidadBeneficiariaAsignada().getId());
    }

    private Bien toBien(BienRequest bienRequest) {
        if (bienRequest.descripcion() == null || bienRequest.descripcion().isBlank()) {
            throw new BusinessException("La descripcion del bien no puede ser nula ni estar vacia");
        }
        if (bienRequest.cantidad() == null || bienRequest.cantidad() <= 0) {
            throw new BusinessException("La cantidad del bien debe ser mayor a 0");
        }
        if (bienRequest.pesoKgPorUnidad() == null || bienRequest.pesoKgPorUnidad() <= 0) {
            throw new BusinessException("El peso del bien debe ser mayor a 0");
        }
        if (bienRequest.volumenM3PorUnidad() == null || bienRequest.volumenM3PorUnidad() <= 0) {
            throw new BusinessException("El volumen del bien debe ser mayor a 0");
        }
        return new Bien(bienRequest.descripcion(), bienRequest.cantidad(), bienRequest.pesoKgPorUnidad(),
                bienRequest.volumenM3PorUnidad(), toSubcategoria(bienRequest.subcategoria()),
                toEstadoBien(bienRequest.estado()), bienRequest.fechaVencimiento());
    }

    private BienResponse toBienResonse(Bien b) {
        return new BienResponse(
                b.getDescripcion(),
                b.getCantidad(),
                b.getPesoKgPorUnidad(),
                b.getVolumenM3PorUnidad());
    }

    private TipoEstadoDonacion toTipoEstadoDonacion(String estado) {
        try {
            return TipoEstadoDonacion.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Estado de donacion '" + estado + "' no valido");
        }
    }

    private EstadoBien toEstadoBien(String estado) {
        try {
            return EstadoBien.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Estado del bien '" + estado + "' no valido");
        }
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

}
