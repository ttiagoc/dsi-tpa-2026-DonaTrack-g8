package ar.edu.utn.frba.ddsi.logistica.services;

import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.CamionActivoResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.ObtenerCamionesActivosResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.ParadaPendienteResponse;
import java.time.LocalDateTime;

import java.util.List;
import org.springframework.stereotype.Service;


import ar.edu.utn.frba.ddsi.common.models.enums.EstadoRuta;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.ObtenerUbicacionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.RecibirTelemetriaRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ubicacion;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;

@Service
public class MonitoreoService {

  private final CamionRepository camionRepository;
  private final RutaRepository rutaRepository;

  public MonitoreoService(CamionRepository camionRepository, RutaRepository rutaRepository) {
    this.camionRepository = camionRepository;
    this.rutaRepository = rutaRepository;
  }

  public void actualizarUbicacionCamion(String patente, RecibirTelemetriaRequest request) {
    Ubicacion nuevaUbicacion = new Ubicacion();
    nuevaUbicacion.setLatitud(request.latitud());
    nuevaUbicacion.setLongitud(request.longitud());
    nuevaUbicacion.setVelocidad(request.velocidad());
    nuevaUbicacion.setTimestamp(request.timestamp() != null ? request.timestamp() : LocalDateTime.now());

    Camion camion = camionRepository.findByPatente(patente)
        .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado: " + patente));

    Ruta ruta = rutaRepository.buscarRutaDelCamion(camion.getId());
    if (ruta.getEstado() != EstadoRuta.EN_TRASLADO) {
      throw new IllegalStateException("El camión no tiene ninguna ruta en estado EN_TRASLADO.");
    }

    ruta.actualizarUbicacion(nuevaUbicacion);
    rutaRepository.save(ruta);
  }

  public ObtenerUbicacionResponse obtenerUltimaUbicacionPorRuta(Long rutaId) {
    Ruta ruta = rutaRepository.findById(rutaId)
        .orElseThrow(() -> new IllegalArgumentException("Ruta no encontrada: " + rutaId));

    if (ruta.getUltimaUbicacion() == null) {
      throw new IllegalStateException("No se han registrado ubicaciones para esta ruta.");
    }

    Ubicacion ubicacion = ruta.getUltimaUbicacion();
    return new ObtenerUbicacionResponse(
        ubicacion.getLatitud(),
        ubicacion.getLongitud(),
        ubicacion.getTimestamp(),
        ubicacion.getVelocidad()
    );
  }

  public ObtenerCamionesActivosResponse obtenerCamionesActivos() {
    List<Ruta> rutasActivas = rutaRepository.buscarRutasActivas();

    List<CamionActivoResponse> activos = rutasActivas.stream().map(ruta -> {
      Camion camion = ruta.getCamion();
      Ubicacion ubi = ruta.getUltimaUbicacion();

      List<ParadaPendienteResponse> pendientes = ruta.getParadas().stream()
          .map(p -> new ParadaPendienteResponse(p.getOrden(), p.getDestino()))
          .toList();

      return new CamionActivoResponse(
          camion.getId(),
          camion.getPatente(),
          (ubi != null) ? ubi.getLatitud() : null,
          (ubi != null) ? ubi.getLongitud() : null,
          (ubi != null) ? ubi.getVelocidad() : null,
          pendientes
      );
    }).toList();

    return new ObtenerCamionesActivosResponse(activos);
  }
}