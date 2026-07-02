package ar.edu.utn.frba.ddsi.logistica.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoRuta;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.CamionActivoResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.ParadaPendienteResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionResponse;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ubicacion;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;
import ar.edu.utn.frba.ddsi.logistica.services.MonitoreoService;

@Service
public class MonitoreoServiceImpl implements MonitoreoService {

  private final CamionRepository camionRepository;
  private final RutaRepository rutaRepository;

  public MonitoreoServiceImpl(CamionRepository camionRepository, RutaRepository rutaRepository) {
    this.camionRepository = camionRepository;
    this.rutaRepository = rutaRepository;
  }

  public void actualizarUbicacionCamion(String patente, UbicacionRequest request) {
    Camion camion = camionRepository.findByPatente(patente)
        .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado: " + patente));

    Ruta ruta = rutaRepository.buscarRutaDelCamion(camion.getId());
    if (ruta.getEstado() != EstadoRuta.EN_TRASLADO) {
      throw new IllegalStateException("El camión no tiene ninguna ruta en estado EN_TRASLADO.");
    }

    ruta.actualizarUbicacion(toUbicacion(request));
    rutaRepository.save(ruta);
  }

  public UbicacionResponse obtenerUltimaUbicacionPorRuta(Long rutaId) {
    Ruta ruta = rutaRepository.findById(rutaId)
        .orElseThrow(() -> new IllegalArgumentException("Ruta no encontrada: " + rutaId));

    if (ruta.getUltimaUbicacion() == null) {
      throw new IllegalStateException("No se han registrado ubicaciones para esta ruta.");
    }

    return this.toUbicacionResponse(ruta.getUltimaUbicacion());
  }

  public List<CamionActivoResponse> obtenerCamionesActivos() {
    List<Ruta> rutasActivas = rutaRepository.buscarRutasActivas();

    return rutasActivas.stream()
        .map(this::toCamionActivoResponse)
        .collect(Collectors.toList());
  }

  private Ubicacion toUbicacion(UbicacionRequest request) {
    Ubicacion ubicacion = new Ubicacion();
    ubicacion.setLatitud(request.latitud());
    ubicacion.setLongitud(request.longitud());
    ubicacion.setVelocidad(request.velocidad());
    ubicacion.setTimestamp(request.timestamp() != null ? request.timestamp() : LocalDateTime.now());
    return ubicacion;
  }

  private UbicacionResponse toUbicacionResponse(Ubicacion ubicacion) {
    return new UbicacionResponse(
        ubicacion.getLatitud(),
        ubicacion.getLongitud(),
        ubicacion.getTimestamp(),
        ubicacion.getVelocidad());
  }

  private CamionActivoResponse toCamionActivoResponse(Ruta ruta) {
    Camion camion = ruta.getCamion();
    Ubicacion ubi = ruta.getUltimaUbicacion();
    List<ParadaPendienteResponse> pendientes = ruta.getParadas().stream().map(this::toParadaPendienteResponse)
        .collect(Collectors.toList());

    return new CamionActivoResponse(
        camion.getId(),
        camion.getPatente(),
        ubi.getLatitud(),
        ubi.getLongitud(),
        ubi.getVelocidad(),
        pendientes);
  }

  private ParadaPendienteResponse toParadaPendienteResponse(Parada parada) {
    return new ParadaPendienteResponse(
        parada.getOrden(),
        parada.getDestino());
  }
}