package ar.edu.utn.frba.ddsi.logistica.services;

import java.net.URI;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;
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

        notificarInicioRuta(ruta);
    }

    private void notificarInicioRuta(Ruta ruta) {
        List<Parada> paradas = ruta.getParadas();

        for (Parada parada : paradas) {
            List<DonacionDTO> donaciones = parada.getEntregas();
            notificarInicioRutaEntidad(parada.getEntidad().getId());
            for (DonacionDTO donacion : donaciones) {
                notificarInicioRutaDonacion(donacion.getId());
            }
        }
    }

    private void notificarInicioRutaDonacion(Long donacionId) {
        URI url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/notificacion-evento/inicio-ruta-donante/")
                .buildAndExpand(donacionId)
                .toUri();
        try {
            restTemplate.postForEntity(url, null, String.class);
        } catch (Exception e) {
            System.err.println("ERROR: Falló la notificacion de inicio de ruta con donacion ID: " + donacionId);
        }
    }

    private void notificarInicioRutaEntidad(Long entidadId) {
        URI url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/notificacion-evento/inicio-ruta-entidad/")
                .buildAndExpand(entidadId)
                .toUri();
        try {
            restTemplate.postForEntity(url, null, String.class);
        } catch (Exception e) {
            System.err.println("ERROR: Falló la notificacion de inicio de ruta con entidad ID: " + entidadId);
        }
    }

    public void confirmarEntregaExitosa(Long donacionId) {
        URI url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/notificacion-evento/confirmacion-entrega-exitosa/")
                .buildAndExpand(donacionId)
                .toUri();
        try {
            restTemplate.postForEntity(url, null, String.class);
        } catch (Exception e) {
            System.err.println(
                    "ERROR: Falló la notificacion de confirmacion de entrega exitosa con donacion ID: " + donacionId);
        }
    }
}
