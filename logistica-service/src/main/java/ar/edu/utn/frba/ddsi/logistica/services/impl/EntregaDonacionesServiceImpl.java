package ar.edu.utn.frba.ddsi.logistica.services.impl;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones.InicioRutaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.ParadaRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.eventos.EventManagerLogistica;
import ar.edu.utn.frba.ddsi.logistica.models.entities.eventos.EventoInicioRuta;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;
import ar.edu.utn.frba.ddsi.logistica.services.EntregaDonacionesService;

@Service
public class EntregaDonacionesServiceImpl implements EntregaDonacionesService {

    private final RutaRepository rutaRepository;
    private final RestTemplate restTemplate;
    private final RestLogisticaConfig properties;
    private final EventManagerLogistica eventManager;

    public EntregaDonacionesServiceImpl(RutaRepository rutaRepository, RestTemplate restTemplate,
            RestLogisticaConfig properties, EventManagerLogistica eventManager) {
        this.rutaRepository = rutaRepository;
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.eventManager = eventManager;
    }

    public void iniciarRuta(Long rutaId) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + rutaId));

        ruta.iniciar();
        rutaRepository.save(ruta);

        InicioRutaRequest inicioRutaRequest = mapToInicioRutaRequest(ruta);
        URI url = UriComponentsBuilder.fromUriString(properties.getDonacionesUrl())
                .path("/donaciones-service/evento/inicio-ruta")
                .build().toUri();
        try {
            restTemplate.postForObject(url, inicioRutaRequest, Void.class);
        } catch (Exception e) {
            System.err.println("ERROR: Falló la notificación masiva de inicio de ruta.");
        }

        eventManager.emitir(
                new EventoInicioRuta(ruta, properties.getLogisticaUrl() + "/monitoreo/ubicacion/" + ruta.getId()));
    }

    private InicioRutaRequest mapToInicioRutaRequest(Ruta ruta) {
        List<ParadaRequest> paradasRequests = new ArrayList<>();
        for (Parada parada : ruta.getParadas()) {
            paradasRequests.add(new ParadaRequest(parada.getOrden(), parada.getDestino(), parada.getEntidadId(),
                    parada.getDonacionIds()));
        }

        return new InicioRutaRequest(ruta.getId(), paradasRequests);
    }

    public void confirmarEntregaExitosa(Long paradaId, Long rutaId) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + rutaId));

        Parada paradaAfectada = ruta.getParadas().stream()
                .filter(p -> p.getOrden() == paradaId.intValue())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una parada con el id: " + paradaId + " en la ruta " + rutaId));

        ConfirmacionEntregaExitosaRequest request = mapToConfirmacionEntregaExitosaRequest(paradaAfectada, ruta);
        URI url = UriComponentsBuilder.fromUriString(properties.getDonacionesUrl())
                .path("/donaciones-service/evento/confirmacion-entrega-exitosa")
                .build().toUri();
        try {
            restTemplate.postForObject(url, request, Void.class);
            System.out.println("Reporte de entrega exitosa enviado a Donaciones para Parada ID: " + paradaId);
        } catch (Exception e) {
            System.err.println("ERROR: No se pudo transmitir la confirmación de entrega por red.");
        }
    }

    private ConfirmacionEntregaExitosaRequest mapToConfirmacionEntregaExitosaRequest(Parada paradaAfectada, Ruta ruta) {
        return new ConfirmacionEntregaExitosaRequest(
                paradaAfectada.getEntidadId(), paradaAfectada.getDonacionIds(),
                ruta.getCamion().getPatente(), LocalDateTime.now());
    }
}