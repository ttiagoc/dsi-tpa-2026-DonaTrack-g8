package ar.edu.utn.frba.ddsi.logistica.services;

import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AdapterPlanificadorExternoImpl implements AdapterPlanificadorExterno {

    @Override
    public void solicitarPlanificacionAsync(List<DonacionDTO> lote, List<Camion> camiones) {
        System.out.println("ADAPTER [HTTP POST]: Transmitiendo lote de " + lote.size()
                + " donaciones al optimizador de rutas externo de forma asincrónica...");
        System.out.println("ADAPTER [HTTP POST]: Flota disponible informada: " + camiones.size() + " camiones.");
        System.out.println("ADAPTER: Petición enviada con éxito. Esperando respuesta por callback.");
    }
}