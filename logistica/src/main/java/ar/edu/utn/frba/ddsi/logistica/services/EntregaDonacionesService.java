package ar.edu.utn.frba.ddsi.logistica.services;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones.InicioRutaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones.ParadaInfo;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;

@Service
public class EntregaDonacionesService {

    private final RutaRepository rutaRepository;
    private final RestTemplate restTemplate;
    private final RestLogisticaConfig properties;

    public EntregaDonacionesService(RutaRepository rutaRepository, RestTemplate restTemplate,
            RestLogisticaConfig properties) {
        this.rutaRepository = rutaRepository;
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public void iniciarRuta(Long rutaId) {

        Ruta ruta = rutaRepository.findById(rutaId).orElseThrow(() -> new IllegalArgumentException("Ruta no encontrada."));

        ruta.iniciar();
        rutaRepository.save(ruta);

        List<ParadaInfo> paradas = new ArrayList<>();
        for (Parada parada : ruta.getParadas()) {
            paradas.add(new ParadaInfo(parada.getEntidad(), parada.getEntregas()));
        }
        
        InicioRutaRequest inicioRutaRequest = new InicioRutaRequest(ruta.getId(), paradas);

        URI url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/donaciones/evento/inicio-ruta")
                .build().toUri();
        try {
            restTemplate.postForEntity(url, inicioRutaRequest, String.class);
        } catch (Exception e) {
            System.err.println("ERROR: Falló la notificación masiva de inicio de ruta.");
        }
    }

    public void confirmarEntregaExitosa(Long paradaId, Long rutaId) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new IllegalArgumentException("Ruta no encontrada."));

        Parada paradaAfectada = ruta.getParadas().stream()
                .filter(p -> p.getOrden() == paradaId.intValue())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Parada no encontrada en la ruta especificada."));

        List<Long> donaciones = paradaAfectada.getEntregas();

        ConfirmacionEntregaExitosaRequest request = new ConfirmacionEntregaExitosaRequest(
                paradaAfectada.getEntidad(), donaciones,
                ruta.getCamion().getPatente(), LocalDateTime.now());

        URI url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/donaciones/evento/confirmacion-entrega-exitosa")
                .build().toUri();
        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("Reporte de entrega exitosa enviado a Donaciones para Parada ID #" + paradaId);
        } catch (Exception e) {
            System.err.println("ERROR: No se pudo transmitir la confirmación de entrega por red.");
        }
    }
}
