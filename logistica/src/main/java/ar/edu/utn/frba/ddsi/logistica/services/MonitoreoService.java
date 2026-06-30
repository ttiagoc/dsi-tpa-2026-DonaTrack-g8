package ar.edu.utn.frba.ddsi.logistica.services;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoRuta;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ubicacion;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MonitoreoService {

  private final CamionRepository camionRepository;
  private final RutaRepository rutaRepository;

  public MonitoreoService(CamionRepository camionRepository, RutaRepository rutaRepository) {
    this.camionRepository = camionRepository;
    this.rutaRepository = rutaRepository;
  }

  public void actualizarUbicacionCamion(String patente, Ubicacion nuevaUbicacion) {
    Camion camion = camionRepository.findByPatente(patente)
        .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado: " + patente));

    List<Ruta> rutasActivas = rutaRepository.buscarRutasActivasPorCamion(camion.getId());

    Ruta ruta = rutasActivas.stream()
        .filter(r -> r.getEstado() == EstadoRuta.EN_TRASLADO)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("El camión no tiene ninguna ruta en estado EN_TRASLADO."));

    ruta.actualizarUbicacion(nuevaUbicacion);
    rutaRepository.save(ruta);
  }
}