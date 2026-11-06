package ar.edu.utn.frba.ddsi.logistica.services.impl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.ParadaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.ParadaResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaResponse;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.GeocodificadorCaba;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.GestorDeRutas;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.MonitorDeRutas;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ubicacion;
import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoParada;
import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoRuta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;
import ar.edu.utn.frba.ddsi.logistica.services.RutaService;

@Service
public class RutaServiceImpl implements RutaService {

    private final RutaRepository rutaRepository;
    private final CamionRepository camionRepository;
    private final GestorDeRutas gestorDeRutas;
    private final MonitorDeRutas monitorDeRutas;

    public RutaServiceImpl(RutaRepository rutaRepository, CamionRepository camionRepository,
            GestorDeRutas gestorDeRutas, MonitorDeRutas monitorDeRutas) {
        this.rutaRepository = rutaRepository;
        this.camionRepository = camionRepository;
        this.gestorDeRutas = gestorDeRutas;
        this.monitorDeRutas = monitorDeRutas;
    }

    public List<RutaResponse> obtenerTodas(LocalDate fecha) {
        List<Ruta> rutas = rutaRepository.findAll();
        if (fecha != null) {
            rutas = rutas.stream()
                    .filter(r -> fecha.equals(r.getFecha()))
                    .collect(Collectors.toList());
        }

        return rutas.stream()
                .map(this::toRutaResponse)
                .collect(Collectors.toList());
    }

    public RutaResponse obtenerPorId(Long id) {
        Ruta r = rutaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + id));

        return this.toRutaResponse(r);
    }

    @Override
    public Optional<RutaResponse> obtenerRutaActivaPorCamion(Long camionId) {
        List<Ruta> rutasDelCamion = rutaRepository.findAll().stream()
                .filter(r -> r.getCamion() != null && camionId.equals(r.getCamion().getId()))
                .toList();

        Optional<Ruta> enTraslado = rutasDelCamion.stream()
                .filter(r -> r.getEstado() == EstadoRuta.EN_TRASLADO)
                .sorted(Comparator.comparing(Ruta::getFecha, Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst();
        if (enTraslado.isPresent()) {
            return enTraslado.map(this::toRutaResponse);
        }

        return rutasDelCamion.stream()
                .filter(r -> r.getEstado() == EstadoRuta.PLANIFICADA)
                .sorted(Comparator.comparing(Ruta::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(r -> r.getId() != null ? r.getId() : Long.MAX_VALUE))
                .findFirst()
                .map(this::toRutaResponse);
    }

    @Override
    public List<RutaResponse> obtenerPorCamion(Long camionId) {
        return rutaRepository.findAll().stream()
                .filter(r -> r.getCamion() != null && camionId.equals(r.getCamion().getId()))
                .sorted(Comparator.comparing(Ruta::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toRutaResponse)
                .toList();
    }

    public boolean eliminar(Long id) {
        return rutaRepository.deleteById(id);
    }

    public RutaResponse crear(RutaRequest request) {
        Ruta ruta = toRuta(request);
        ruta = rutaRepository.save(ruta);
        return this.toRutaResponse(ruta);
    }

    public RutaResponse actualizar(Long id, RutaRequest request) {
        Ruta existente = rutaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + id));
        if (request.fecha() != null) {
            existente.setFecha(request.fecha());
        }
        if (request.idCamion() != null) {
            Camion camion = camionRepository.findById(request.idCamion())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontro un camion con el id: " + request.idCamion()));
            existente.setCamion(camion);
        }
        if (request.paradas() != null && !request.paradas().isEmpty()) {
            existente.setParadas(request.paradas().stream().map(this::toParada).collect(Collectors.toList()));
        }

        existente = rutaRepository.save(existente);
        return this.toRutaResponse(existente);
    }

    private RutaResponse toRutaResponse(Ruta ruta) {
        Camion camion = ruta.getCamion();
        String patenteCamion = camion != null ? camion.getPatente() : null;
        Long camionId = camion != null ? camion.getId() : null;

        Parada ultimaCompletada = null;
        if (ruta.getParadas() != null) {
            ultimaCompletada = ruta.getParadas().stream()
                    .filter(p -> p.getEstado() == EstadoParada.ENTREGADA || p.getEstado() == EstadoParada.NO_RECIBIDA)
                    .max(Comparator.comparing(Parada::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
        }

        Double camionLat;
        Double camionLng;
        if (ultimaCompletada != null) {
            double[] coords = ultimaCompletada.coordenadas();
            camionLat = coords[0];
            camionLng = coords[1];
        } else if (ruta.getEstado() == EstadoRuta.PLANIFICADA || (ruta.getEstado() == EstadoRuta.EN_TRASLADO && ultimaCompletada == null)) {
            camionLat = GeocodificadorCaba.DEPOSITO_LAT;
            camionLng = GeocodificadorCaba.DEPOSITO_LNG;
        } else if (camion != null && camion.getUbicacion() != null) {
            camionLat = camion.getUbicacion().getLatitud();
            camionLng = camion.getUbicacion().getLongitud();
        } else {
            camionLat = GeocodificadorCaba.DEPOSITO_LAT;
            camionLng = GeocodificadorCaba.DEPOSITO_LNG;
        }
        String choferNombre = ruta.getChofer() != null ? ruta.getChofer().getNombre() : null;
        String choferApellido = ruta.getChofer() != null ? ruta.getChofer().getApellido() : null;

        List<ParadaResponse> paradas = ruta.getParadas() != null
                ? ruta.getParadas().stream().map(this::toParadaResponse).collect(Collectors.toList())
                : null;

        return new RutaResponse(
                ruta.getId(),
                ruta.getFecha(),
                ruta.getEstado(),
                patenteCamion,
                paradas,
                choferNombre,
                choferApellido,
                camionId,
                camionLat,
                camionLng);
    }

    private ParadaResponse toParadaResponse(Parada parada) {
        double[] coords = parada.coordenadas();
        return new ParadaResponse(
                parada.getId(),
                parada.getOrden(),
                parada.getDestino(),
                parada.getEntidadId(),
                parada.getDonacionIds(),
                parada.getEstado(),
                coords[0],
                coords[1]);
    }

    private Ruta toRuta(RutaRequest request) {
        if (request.fecha() == null) {
            throw new BusinessException("La fecha de la ruta no puede ser nula");
        }
        if (request.paradas() == null || request.paradas().isEmpty()) {
            throw new BusinessException("La ruta debe tener al menos una parada");
        }

        Camion camion = camionRepository.findById(request.idCamion())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro un camion con el id: " + request.idCamion()));
        List<Parada> paradas = request.paradas().stream().map(this::toParada).collect(Collectors.toList());

        return new Ruta(request.fecha(), camion, paradas);
    }

    private Parada toParada(ParadaRequest request) {
        if (request.orden() == null || request.orden() < 0) {
            throw new BusinessException("El orden de la parada debe ser mayor o igual a 0");
        }
        if (request.destino() == null || request.destino().isBlank()) {
            throw new BusinessException("El destino de la parada no puede ser nulo ni estar vacio");
        }
        if (request.entidad() == null) {
            throw new BusinessException("La entidad de la parada no puede ser nula");
        }
        if (request.entregas() == null || request.entregas().isEmpty()) {
            throw new BusinessException("La parada debe tener al menos una entrega");
        }

        Parada parada = new Parada();
        parada.setOrden(request.orden());
        parada.setDestino(request.destino());
        parada.setEntidadId(request.entidad());
        parada.setDonacionIds(request.entregas());
        return parada;
    }

    public void actualizarEstado(Long id, String estado) {
        Ruta ruta = rutaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + id));

        if ("EN_TRASLADO".equalsIgnoreCase(estado)) {
            ruta.iniciar();
            if (ruta.getCamion() != null) {
                // Al salir a recorrido, la posición inicial del camión es el depósito central
                ruta.getCamion().actualizarUbicacion(
                        new Ubicacion(GeocodificadorCaba.DEPOSITO_LAT, GeocodificadorCaba.DEPOSITO_LNG, 0.0));
                camionRepository.save(ruta.getCamion());
            }
            rutaRepository.save(ruta);
            gestorDeRutas.iniciarRuta(ruta);
        } else if ("FINALIZADA".equalsIgnoreCase(estado)) {
            ruta.finalizar();
            rutaRepository.save(ruta);
        } else if ("PLANIFICADA".equalsIgnoreCase(estado)) {
            ruta.setEstado(EstadoRuta.PLANIFICADA);
            rutaRepository.save(ruta);
        } else {
            throw new BusinessException("Estado no soportado: " + estado);
        }
    }

    public void confirmarEntregaExitosa(Long rutaId, Long paradaId) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + rutaId));
        if (ruta.getEstado() != EstadoRuta.EN_TRASLADO) {
            throw new BusinessException("Primero se debe iniciar el recorrido de la ruta antes de confirmar paradas");
        }

        Parada parada = ruta.getParadas().stream()
                .filter(p -> paradaId.equals(p.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una parada con el id: " + paradaId + " en la ruta " + rutaId));

        parada.marcarEntregada();
        moverCamionAParada(ruta, parada);
        if (ruta.todasLasParadasResueltas()) {
            ruta.finalizar();
        }
        rutaRepository.save(ruta);

        gestorDeRutas.confirmarEntregaExitosa(parada, ruta);
    }

    @Override
    public void registrarEntregaNoRecibida(Long rutaId, Long paradaId) {
        registrarEntregaNoRecibida(rutaId, paradaId, null);
    }

    @Override
    public void registrarEntregaNoRecibida(Long rutaId, Long paradaId, String motivo) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + rutaId));
        if (ruta.getEstado() != EstadoRuta.EN_TRASLADO) {
            throw new BusinessException("Primero se debe iniciar el recorrido de la ruta antes de registrar fallos");
        }

        Parada parada = ruta.getParadas().stream()
                .filter(p -> paradaId.equals(p.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una parada con el id: " + paradaId + " en la ruta " + rutaId));

        parada.marcarNoRecibida();
        moverCamionAParada(ruta, parada);
        if (ruta.todasLasParadasResueltas()) {
            ruta.finalizar();
        }
        rutaRepository.save(ruta);

        gestorDeRutas.registrarEntregaFallida(parada, motivo);
    }

    private void moverCamionAParada(Ruta ruta, Parada parada) {
        if (ruta.getCamion() == null) {
            return;
        }
        double[] coords = parada.coordenadas();
        ruta.getCamion().actualizarUbicacion(new Ubicacion(coords[0], coords[1], 0.0));
        camionRepository.save(ruta.getCamion());
    }

    public UbicacionResponse obtenerUbicacionActual(Long id) {
        return monitorDeRutas.obtenerUltimaUbicacionPorRuta(id);
    }

    @Override
    public List<ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaSeguimientoResponse> obtenerRutasActivas(Long entidadId, List<Long> donacionIds) {
      List<Ruta> rutas = rutaRepository.buscarRutasActivasFiltradas(entidadId, donacionIds);
      return rutas.stream()
          .map(this::toRutaSeguimientoResponse)
          .collect(Collectors.toList());
    }

    private ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaSeguimientoResponse toRutaSeguimientoResponse(Ruta ruta) {
      Camion camion = ruta.getCamion();
      String patente = camion != null ? camion.getPatente() : null;
      String chofer = (camion != null && camion.getChofer() != null)
          ? camion.getChofer().getNombre() + " " + camion.getChofer().getApellido()
          : null;

      Double latitud = null;
      Double longitud = null;
      Double velocidad = null;
      java.time.LocalDateTime ultimaActualizacion = null;

      Parada ultimaCompletada = null;
      if (ruta.getParadas() != null) {
          ultimaCompletada = ruta.getParadas().stream()
                  .filter(p -> p.getEstado() == EstadoParada.ENTREGADA || p.getEstado() == EstadoParada.NO_RECIBIDA)
                  .max(Comparator.comparing(Parada::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                  .orElse(null);
      }

      if (camion != null && camion.getUbicacion() != null) {
        latitud = camion.getUbicacion().getLatitud();
        longitud = camion.getUbicacion().getLongitud();
        velocidad = camion.getUbicacion().getVelocidad();
        ultimaActualizacion = camion.getUbicacion().getTimestamp();
      } else if (ultimaCompletada != null) {
        double[] coords = ultimaCompletada.coordenadas();
        latitud = coords[0];
        longitud = coords[1];
      } else {
        latitud = GeocodificadorCaba.DEPOSITO_LAT;
        longitud = GeocodificadorCaba.DEPOSITO_LNG;
      }

      List<ParadaResponse> paradas = ruta.getParadas().stream()
          .map(this::toParadaResponse)
          .collect(Collectors.toList());

      return new ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaSeguimientoResponse(
          ruta.getId(),
          patente,
          chofer,
          latitud,
          longitud,
          velocidad,
          ultimaActualizacion,
          paradas
      );
    }
}