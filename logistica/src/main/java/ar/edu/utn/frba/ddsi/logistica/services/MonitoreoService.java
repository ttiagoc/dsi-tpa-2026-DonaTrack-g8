package ar.edu.utn.frba.ddsi.logistica.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoRuta;
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

  public void actualizarUbicacionCamion(String patente, Ubicacion nuevaUbicacion) {
    if (nuevaUbicacion.getTimestamp() == null) {
      nuevaUbicacion.setTimestamp(LocalDateTime.now());
    }

    Camion camion = camionRepository.findByPatente(patente)
        .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado: " + patente));

    Ruta ruta = rutaRepository.buscarRutaDelCamion(camion.getId());
    if (ruta.getEstado() != EstadoRuta.EN_TRASLADO) {
      throw new IllegalStateException("El camión no tiene ninguna ruta en estado EN_TRASLADO.");
    }

    ruta.actualizarUbicacion(nuevaUbicacion);
    rutaRepository.save(ruta);
  }

  public Ubicacion obtenerUltimaUbicacionPorRuta(Long rutaId) {
    Ruta ruta = rutaRepository.findById(rutaId)
        .orElseThrow(() -> new IllegalArgumentException("Ruta no encontrada: " + rutaId));

    if (ruta.getUltimaUbicacion() == null) {
      throw new IllegalStateException("No se han registrado ubicaciones para esta ruta.");
    }

    return ruta.getUltimaUbicacion();
  }
}