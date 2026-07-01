package ar.edu.utn.frba.ddsi.donaciones.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.BienRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.BienResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionAsignadaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.SegmentadorDeDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;

@Service
public class DonacionService {

    private final DonacionRepository donacionRepository;
    private final SegmentadorDeDonacion segmentadorDeDonacion;

    public DonacionService(DonacionRepository donacionRepository, SegmentadorDeDonacion segmentadorDeDonacion) {
        this.donacionRepository = donacionRepository;
        this.segmentadorDeDonacion = segmentadorDeDonacion;
    }

    public List<DonacionResponse> obtenerTodas() {
        return donacionRepository.findAll().stream()
                .map(this::toDonacionResponse)
                .collect(Collectors.toList());
    }

    public DonacionResponse obtenerPorId(Long id) {
        Donacion d = donacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));

        return this.toDonacionResponse(d);
    }

    public List<DonacionResponse> crear(DonacionRequest request) {
        RegistroDonacion registro = toRegistroDonacion(request);
        List<Donacion> donacionesCreadas = segmentadorDeDonacion.segmentarDonacion(registro);
        donacionesCreadas.forEach(d -> d.setId(null));
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
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
        TipoEstadoDonacion nuevoEstado = toTipoEstadoDonacion(request.estado());
        donacion.cambiarEstado(nuevoEstado, request.justificacion());
        donacion = donacionRepository.save(donacion);

        return new EstadoDonacionResponse(
                donacion.getId(),
                donacion.estadoActual().toString(),
                donacion.getHistorialEstados().get(donacion.getHistorialEstados().size() - 1).getFecha());
    }

    public List<DonacionAsignadaResponse> obtenerDonacionesAsignadas(int limit) {
        List<Donacion> donaciones = donacionRepository.buscarPorEstado(TipoEstadoDonacion.ASIGNACION_REALIZADA);
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

    public void donacionesEntregaLista(List<Long> donacionesIds) {
        for (Long donacionId : donacionesIds) {
            Donacion donacion = donacionRepository.findById(donacionId)
                    .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
            donacion.cambiarEstado(TipoEstadoDonacion.LISTA_PARA_ENTREGAR, null);
            donacionRepository.save(donacion);
        }
    }

    public void replanificar(Long id) {
        Donacion donacion = donacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
        donacion.cambiarEstado(TipoEstadoDonacion.EN_DEPOSITO, "Replanificación de entrega fallida");
        donacionRepository.save(donacion);
    }

    public Donacion guardar(Donacion donacion) {
        return donacionRepository.save(donacion);
    }

    private RegistroDonacion toRegistroDonacion(DonacionRequest donacionRequest) {
        RegistroDonacion registroDonacion = new RegistroDonacion();
        registroDonacion.setFecha(LocalDateTime.now());
        if (donacionRequest.bienes() == null || donacionRequest.bienes().isEmpty()) {
            throw new IllegalArgumentException("La donacion debe tener al menos un bien");
        }
        registroDonacion.setBienes(donacionRequest.bienes().stream().map(this::toBien).collect(Collectors.toList()));
        return registroDonacion;
    }

    private DonacionResponse toDonacionResponse(Donacion d) {
        return new DonacionResponse(
                d.getId(),
                d.getBienes().stream().map(this::toBienInfo).collect(Collectors.toList()),
                d.estadoActual().toString(),
                d.getFecha(),
                d.getDonante().getId(),
                d.getEntidadBeneficiariaAsignada().getId());
    }

    private Bien toBien(BienRequest bienRequest) {
        Bien bien = new Bien();
        bien.setDescripcion(bienRequest.descripcion());
        bien.setCantidad(bienRequest.cantidad());
        bien.setPesoKgPorUnidad(bienRequest.pesoKgPorUnidad());
        bien.setVolumenM3PorUnidad(bienRequest.volumenM3PorUnidad());
        bien.setEstadoBien(toEstadoBien(bienRequest.estado()));
        bien.setFechaVencimiento(bienRequest.fechaVencimiento());
        bien.setSubcategoria(toSubcategoria(bienRequest.subcategoria()));
        return bien;
    }

    private BienResponse toBienInfo(Bien b) {
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
            throw new IllegalArgumentException("Estado del bien no valido");
        }
    }

    private EstadoBien toEstadoBien(String estado) {
        try {
            return EstadoBien.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado del bien no valido");
        }
    }

    private Subcategoria toSubcategoria(SubcategoriaRequest subcategoria) {
        return new Subcategoria(subcategoria.nombre(), toCategoria(subcategoria.categoria()));
    }

    private Categoria toCategoria(CategoriaRequest categoria) {
        return new Categoria(categoria.nombre(), categoria.pideEstado(), categoria.esPerecedero());
    }

}
