package ar.edu.utn.frba.ddsi.logistica.services;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.EntregaExitosaDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.InicioRutaDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.ParadaDTO;
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

        Ruta ruta = rutaRepository.findById(rutaId).get();

        ruta.iniciar();
        rutaRepository.save(ruta);

        InicioRutaDTO inicioRutaDTO = new InicioRutaDTO();
        inicioRutaDTO.setRutaId(ruta.getId());

        List<ParadaDTO> paradas = new ArrayList<>();
        for (Parada parada : ruta.getParadas()) {
            ParadaDTO paradaDTO = new ParadaDTO();
            paradaDTO.setEntidadId(parada.getEntidad());
            paradaDTO.setDonacionIds(parada.getEntregas());
            paradas.add(paradaDTO);
        }
        inicioRutaDTO.setParadas(paradas);

        URI url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/evento/inicio-ruta")
                .build().toUri();
        try {
            restTemplate.postForEntity(url, inicioRutaDTO, String.class);
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

        EntregaExitosaDTO entregaExitosaDTO = new EntregaExitosaDTO(paradaAfectada.getEntidad(), donaciones,
                ruta.getCamion().getPatente(), LocalDateTime.now());

        URI url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/evento/entrega-exitosa")
                .build().toUri();
        try {
            restTemplate.postForEntity(url, entregaExitosaDTO, String.class);
            System.out
                    .println("Reporte de entrega exitosa enviado a Donaciones para Parada ID #" + paradaId);
        } catch (Exception e) {
            System.err.println("ERROR: No se pudo transmitir la confirmación de entrega por red.");
        }
    }
}
