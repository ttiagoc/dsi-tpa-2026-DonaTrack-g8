package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.SegmentadorDeDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;

@Service
public class DonacionService {

    private final DonacionRepository donacionRepository;
    private final SegmentadorDeDonacion segmentadorDeDonacion;

    public DonacionService(DonacionRepository donacionRepository, SegmentadorDeDonacion segmentadorDeDonacion) {
        this.donacionRepository = donacionRepository;
        this.segmentadorDeDonacion = segmentadorDeDonacion;
    }

    public List<ObtenerTodasDonacionesResponse> obtenerTodas() {
        return donacionRepository.findAll().stream()
                .map(d -> new ObtenerTodasDonacionesResponse(
                        d.getId(),
                        d.getSubcategoria().getNombre(),
                        d.estadoActual().toString(),
                        d.getFecha()))
                .collect(Collectors.toList());
    }

    public ObtenerDonacionResponse obtenerPorId(Long id) {
        Donacion d = donacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));

        List<BienInfo> bienesInfo = d.getBienes() != null ? d.getBienes().stream()
                .map(b -> new BienInfo(
                        b.getDescripcion(),
                        b.getCantidad(),
                        b.getPesoKgPorUnidad(),
                        b.getVolumenM3PorUnidad(),
                        b.getSubcategoria().getNombre(),
                        b.getEstadoBien().toString(),
                        b.getFechaVencimiento()))
                .collect(Collectors.toList()) : new ArrayList<>();

        return new ObtenerDonacionResponse(
                d.getId(),
                d.getSubcategoria().getNombre(),
                d.getEstadoBienes().toString(),
                bienesInfo,
                d.estadoActual().toString(),
                d.getFecha(),
                d.getDonante().getId(),
                d.getEntidadBeneficiariaAsignada() != null ? d.getEntidadBeneficiariaAsignada().getId() : null);
    }

    public CrearDonacionResponse crear(CrearDonacionRequest request) {
        RegistroDonacion registro = new RegistroDonacion();
        registro.setDescripcion(request.descripcion());
        registro.setFecha(request.fecha());

        if (request.bienes() != null) {
            List<Bien> bienes = request.bienes().stream().map(bInfo -> {
                Bien b = new Bien();
                b.setDescripcion(bInfo.descripcion());
                b.setCantidad(bInfo.cantidad());
                b.setPesoKgPorUnidad(bInfo.pesoKgPorUnidad());
                b.setVolumenM3PorUnidad(bInfo.volumenM3PorUnidad());
                b.setFechaVencimiento(bInfo.fechaVencimiento());
                if (bInfo.estadoBien() != null) {
                    try {
                        b.setEstadoBien(EstadoBien.valueOf(bInfo.estadoBien()));
                    } catch (IllegalArgumentException e) {
                        // ignore
                    }
                }
                if (bInfo.subcategoria() != null) {
                    Subcategoria sub = new Subcategoria();
                    sub.setNombre(bInfo.subcategoria());
                    b.setSubcategoria(sub);
                }
                return b;
            }).collect(Collectors.toList());
            registro.setBienes(bienes);
        } else {
            registro.setBienes(new ArrayList<>());
        }

        List<Donacion> donacionesCreadas = segmentadorDeDonacion.segmentarDonacion(registro);
        donacionesCreadas.forEach(d -> d.setId(null));
        donacionesCreadas = donacionRepository.saveAll(donacionesCreadas);

        List<DonacionCreadaInfo> donacionesInfo = donacionesCreadas.stream()
                .map(d -> new DonacionCreadaInfo(
                        d.getId(),
                        d.getSubcategoria() != null ? d.getSubcategoria().getNombre() : null,
                        d.estadoActual() != null ? d.estadoActual().toString() : null))
                .collect(Collectors.toList());

        return new CrearDonacionResponse(donacionesInfo);
    }

    public boolean eliminar(Long id) {
        return donacionRepository.deleteById(id);
    }

    public CambiarEstadoDonacionResponse cambiarEstado(Long id, CambiarEstadoDonacionRequest request) {
        Donacion donacion = donacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
        TipoEstadoDonacion nuevoEstado = TipoEstadoDonacion.valueOf(request.estado());
        donacion.cambiarEstado(nuevoEstado, request.justificacion());
        donacion = donacionRepository.save(donacion);

        return new CambiarEstadoDonacionResponse(
                donacion.getId(),
                donacion.estadoActual().toString(),
                donacion.getHistorialEstados().get(donacion.getHistorialEstados().size() - 1).getFecha());
    }

    public ObtenerDonacionesAsignadasResponse obtenerDonacionesAsignadas(int limit) {
        List<Donacion> donaciones = donacionRepository.buscarPorEstado(TipoEstadoDonacion.ASIGNACION_REALIZADA);
        if (donaciones.size() > limit) {
            donaciones = donaciones.subList(0, limit);
        }

        List<DonacionAsignadaInfo> donacionesAsignadas = new ArrayList<>();
        for (Donacion donacion : donaciones) {
            donacionesAsignadas.add(new DonacionAsignadaInfo(
                    donacion.getId(),
                    donacion.calcularPesoTotal(),
                    donacion.calcularVolumenTotal(),
                    donacion.obtenerDireccion()));
        }

        return new ObtenerDonacionesAsignadasResponse(donacionesAsignadas);
    }

    public void donacionesEntregaLista(DonacionesListaEntregaRequest request) {
        if (request.donaciones() != null) {
            for (DonacionEntregaInfo info : request.donaciones()) {
                Donacion donacion = donacionRepository.findById(info.id())
                        .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
                donacion.cambiarEstado(TipoEstadoDonacion.LISTA_PARA_ENTREGAR, null);
                donacionRepository.save(donacion);
            }
        }
    }

    public ReplanificarDonacionResponse replanificar(Long id) {
        Donacion donacion = donacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
        donacion.cambiarEstado(TipoEstadoDonacion.EN_DEPOSITO, "Replanificación de entrega fallida");
        donacion = donacionRepository.save(donacion);
        return new ReplanificarDonacionResponse(donacion.getId(), donacion.estadoActual().toString());
    }

    public Donacion guardar(Donacion donacion) {
        return donacionRepository.save(donacion);
    }

}
