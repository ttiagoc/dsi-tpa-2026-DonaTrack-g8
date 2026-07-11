package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.CamionActivoResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.ParadaPendienteResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionResponse;
import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoRuta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;

@Component
public class MonitorDeRutas {

  private final CamionRepository camionRepository;
  private final RutaRepository rutaRepository;

  public MonitorDeRutas(CamionRepository camionRepository, RutaRepository rutaRepository) {
    this.camionRepository = camionRepository;
    this.rutaRepository = rutaRepository;
  }

  public void actualizarUbicacionCamion(String patente, UbicacionRequest request) {
    Camion camion = camionRepository.findByPatente(patente)
        .orElseThrow(() -> new ResourceNotFoundException("No se encontro un camion con la patente: " + patente));

    Ruta ruta = rutaRepository.buscarRutaDelCamion(camion.getId());
    if (ruta.getEstado() != EstadoRuta.EN_TRASLADO) {
      throw new BusinessException("El camión no tiene ninguna ruta en estado EN_TRASLADO.");
    }

    camion.actualizarUbicacion(toUbicacion(request));
    camionRepository.save(camion);
  }

  public UbicacionResponse obtenerUltimaUbicacionPorRuta(Long rutaId) {
    Ruta ruta = rutaRepository.findById(rutaId)
        .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + rutaId));

    Camion camion = ruta.getCamion();
    if (camion == null || camion.getUbicacion() == null) {
      throw new ResourceNotFoundException("No se han registrado ubicaciones para esta ruta.");
    }

    return this.toUbicacionResponse(camion.getUbicacion());
  }

  public List<CamionActivoResponse> obtenerCamionesActivos() {
    List<Ruta> rutasActivas = rutaRepository.buscarRutasActivas();

    return rutasActivas.stream()
        .map(this::toCamionActivoResponse)
        .collect(Collectors.toList());
  }

  private Ubicacion toUbicacion(UbicacionRequest request) {
    if (request.latitud() == null || request.longitud() == null) {
      throw new BusinessException("La ubicacion es inválida.");
    }
    if (request.velocidad() == null) {
      throw new BusinessException("La velocidad es inválida.");
    }
    return new Ubicacion(request.latitud(), request.longitud(), request.velocidad());
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
    Ubicacion ubi = camion.getUbicacion();
    List<ParadaPendienteResponse> pendientes = ruta.getParadas().stream().map(this::toParadaPendienteResponse)
        .collect(Collectors.toList());

    return new CamionActivoResponse(
        camion.getId(),
        camion.getPatente(),
        ubi != null ? ubi.getLatitud() : null,
        ubi != null ? ubi.getLongitud() : null,
        ubi != null ? ubi.getVelocidad() : null,
        pendientes);
  }

  private ParadaPendienteResponse toParadaPendienteResponse(Parada parada) {
    return new ParadaPendienteResponse(
        parada.getOrden(),
        parada.getDestino());
  }
}
