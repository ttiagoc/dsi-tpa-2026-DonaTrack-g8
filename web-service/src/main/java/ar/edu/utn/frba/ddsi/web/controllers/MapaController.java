package ar.edu.utn.frba.ddsi.web.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.EntidadConEntregas;
import io.javalin.http.Context;

/**
 * Req. 2: mapa publico de las donaciones ya entregadas. Un marcador por entidad (todas sus entregas van a la
 * misma direccion) y, al elegirlo, lo que recibio con las fotos de la recepcion (req. 1: fotos sin login).
 * El mapa es ilustrativo: ver PosicionesEnMapa.
 */
@Component
public class MapaController {

    private static final String VISTA = "home/mapa.html.hbs";
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

    private final DonacionesApiClient donaciones;

    public MapaController(DonacionesApiClient donaciones) {
        this.donaciones = donaciones;
    }

    public void mostrar(Context ctx) {
        List<EntidadConEntregas> entidades = Arrays.asList(donaciones.entregasPorEntidad());
        List<PosicionesEnMapa.Punto> puntos = PosicionesEnMapa.ubicar(
                entidades.stream().map(EntidadConEntregas::entidadId).toList());
        List<EntidadVista> vista = new ArrayList<>();
        for (int i = 0; i < entidades.size(); i++) {
            vista.add(EntidadVista.de(entidades.get(i), puntos.get(i), i == 0));
        }
        Map<String, Object> modelo = Modelo.base(ctx);
        modelo.put("entidades", vista);
        modelo.put("totalEntregas", vista.stream().mapToInt(e -> e.entregas().size()).sum());
        modelo.put("totalEntidades", vista.size());
        ctx.render(VISTA, modelo);
    }

    /** Un marcador y su detalle. La primera (la de la entrega mas reciente) arranca elegida. */
    public record EntidadVista(Long id, String razonSocial, String direccion, int cantidad, String etiquetaCantidad,
            double x, double y, boolean elegida, List<EntregaVista> entregas) {

        static EntidadVista de(EntidadConEntregas e, PosicionesEnMapa.Punto punto, boolean elegida) {
            List<EntregaVista> entregas = e.entregas().stream().map(EntregaVista::de).toList();
            int cantidad = entregas.size();
            return new EntidadVista(e.entidadId(), e.razonSocial(), e.direccion(), cantidad,
                    cantidad == 1 ? "1 donación" : cantidad + " donaciones", punto.x(), punto.y(), elegida, entregas);
        }
    }

    public record EntregaVista(String bienes, String fecha, String categoria, List<String> fotos) {

        static EntregaVista de(EntidadConEntregas.Entrega entrega) {
            String bienes = entrega.bienes() == null ? "" : entrega.bienes().stream()
                    .map(b -> b.descripcion() + " × " + b.cantidad())
                    .collect(Collectors.joining(", "));
            return new EntregaVista(bienes, dia(entrega.fechaEntrega()), entrega.categoria(),
                    entrega.fotos() == null ? List.of() : entrega.fotos());
        }

        private static String dia(LocalDateTime fecha) {
            return fecha == null ? "" : fecha.format(DIA);
        }
    }
}
