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
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.SegmentadorDeDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RegistroDonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.SubcategoriaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;

@Service
public class DonacionServiceImpl implements DonacionService {

    private final DonacionRepository donacionRepository;
    private final SegmentadorDeDonacion segmentadorDeDonacion;
    private final GestorDeEventos gestorDeEventos;
    private final EntidadBeneficiariaRepository entidadBeneficiariaRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final DonanteRepository donanteRepository;
    private final RegistroDonacionRepository registroDonacionRepository;

    public DonacionServiceImpl(DonacionRepository donacionRepository, SegmentadorDeDonacion segmentadorDeDonacion,
            GestorDeEventos gestorDeEventos, EntidadBeneficiariaRepository entidadBeneficiariaRepository,
            SubcategoriaRepository subcategoriaRepository, DonanteRepository donanteRepository,
            RegistroDonacionRepository registroDonacionRepository) {
        this.donacionRepository = donacionRepository;
        this.segmentadorDeDonacion = segmentadorDeDonacion;
        this.gestorDeEventos = gestorDeEventos;
        this.entidadBeneficiariaRepository = entidadBeneficiariaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
        this.donanteRepository = donanteRepository;
        this.registroDonacionRepository = registroDonacionRepository;
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
        RegistroDonacion registro = registroDonacionRepository.save(toRegistroDonacion(request));
        Donante donante = registro.getDonante();
        donante.agregarDonacion(registro);
        donanteRepository.save(donante);

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

    public List<DonacionAsignadaResponse> obtenerDonacionesSegunEstado(String estado, int limit, int offset) {
        TipoEstadoDonacion tipoEstado = toTipoEstadoDonacion(estado);

        List<Donacion> donaciones = donacionRepository.buscarPorEstado(tipoEstado);
        int fromIndex = Math.min(offset, donaciones.size());
        int toIndex = Math.min(offset + limit, donaciones.size());
        donaciones = donaciones.subList(fromIndex, toIndex);

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
        if (donacionRequest.idDonante() == null) {
            throw new BusinessException("La donacion debe indicar el id del donante");
        }
        Donante donante = donanteRepository.findById(donacionRequest.idDonante())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro un donante con el id: " + donacionRequest.idDonante()));

        return new RegistroDonacion(donante, donacionRequest.descripcion(),
                donacionRequest.bienes().stream().map(this::toBien).collect(Collectors.toList()));
    }

    private DonacionResponse toDonacionResponse(Donacion d) {
        // la entidad beneficiaria recien se asigna cuando corre el matchmaking
        Long entidadAsignadaId = d.getEntidadBeneficiariaAsignada() != null
                ? d.getEntidadBeneficiariaAsignada().getId()
                : null;
        Long donanteId = d.getDonante() != null ? d.getDonante().getId() : null;

        return new DonacionResponse(
                d.getId(),
                d.getBienes().stream().map(this::toBienResonse).collect(Collectors.toList()),
                d.estadoActual().toString(),
                d.getFecha(),
                donanteId,
                entidadAsignadaId);
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

    // Aca el estado si es obligatorio: cambiar de estado sin decir a cual no significa nada.
    private TipoEstadoDonacion toTipoEstadoDonacion(String estado) {
        if (estado == null || estado.isBlank()) {
            throw new BusinessException("El estado de la donacion no puede ser nulo ni estar vacio");
        }
        try {
            return TipoEstadoDonacion.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Estado de donacion '" + estado + "' no valido");
        }
    }

    // El estado del bien es opcional: solo lo exigen las categorias con pideEstado = true, y
    // de eso se encarga el constructor de Bien. Aca se traduce lo que vino y nada mas.
    private EstadoBien toEstadoBien(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoBien.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Estado del bien '" + estado + "' no valido");
        }
    }

    // Las subcategorias se cargan previamente desde el catalogo (CategoriaController):
    // un bien solo puede referenciar una subcategoria existente.
    private Subcategoria toSubcategoria(SubcategoriaRequest subcategoria) {
        if (subcategoria == null || subcategoria.nombre() == null || subcategoria.nombre().isBlank()) {
            throw new BusinessException("El nombre de la subcategoria no puede ser nulo ni estar vacio");
        }
        return subcategoriaRepository.findByNombre(subcategoria.nombre())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la subcategoria '" + subcategoria.nombre() + "' en el catalogo"));
    }

}
