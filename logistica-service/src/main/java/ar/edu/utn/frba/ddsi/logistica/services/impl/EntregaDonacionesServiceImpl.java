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
import ar.edu.utn.frba.ddsi.logistica.models.entities.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;
import ar.edu.utn.frba.ddsi.logistica.services.EntregaDonacionesService;

@Service
public class EntregaDonacionesServiceImpl implements EntregaDonacionesService {

    private final RutaRepository rutaRepository;
    private final RestTemplate restTemplate;
    private final RestLogisticaConfig properties;

    public EntregaDonacionesServiceImpl(RutaRepository rutaRepository, RestTemplate restTemplate,
            RestLogisticaConfig properties) {
        this.rutaRepository = rutaRepository;
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public void iniciarRuta(Long rutaId) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + rutaId));

        ruta.iniciar();
        rutaRepository.save(ruta);

        List<ParadaRequest> paradas = new ArrayList<>();
        for (Parada parada : ruta.getParadas()) {
            paradas.add(new ParadaRequest(parada.getOrden(), parada.getDestino(), parada.getEntidad(),
                    parada.getEntregas()));
        }

        InicioRutaRequest inicioRutaRequest = new InicioRutaRequest(ruta.getId(), paradas);

        URI url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/donaciones-service/evento/inicio-ruta")
                .build().toUri();
        try {
            restTemplate.postForObject(url, inicioRutaRequest, Void.class);
        } catch (Exception e) {
            System.err.println("ERROR: Falló la notificación masiva de inicio de ruta.");
        }
    }

    public void confirmarEntregaExitosa(Long paradaId, Long rutaId) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro una ruta con el id: " + rutaId));

        Parada paradaAfectada = ruta.getParadas().stream()
                .filter(p -> p.getOrden() == paradaId.intValue())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una parada con el id: " + paradaId + " en la ruta " + rutaId));

        List<Long> donaciones = paradaAfectada.getEntregas();

        ConfirmacionEntregaExitosaRequest request = new ConfirmacionEntregaExitosaRequest(
                paradaAfectada.getEntidad(), donaciones,
                ruta.getCamion().getPatente(), LocalDateTime.now());

        URI url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/donaciones-service/evento/confirmacion-entrega-exitosa")
                .build().toUri();
        try {
            restTemplate.postForObject(url, request, Void.class);
            System.out.println("Reporte de entrega exitosa enviado a Donaciones para Parada ID #" + paradaId);
        } catch (Exception e) {
            System.err.println("ERROR: No se pudo transmitir la confirmación de entrega por red.");
        }
    }
}