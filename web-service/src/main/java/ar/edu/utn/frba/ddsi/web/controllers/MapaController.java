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
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import io.javalin.http.Context;

/**
 * Req. 2: mapa publico de las donaciones ya entregadas. Un marcador por entidad (todas sus entregas van a la
 * misma direccion) y, al elegirlo, lo que recibio con las fotos de la recepcion (req. 1: fotos sin login).
 * Cada entidad va donde queda su direccion (la API la geocodifica con Georef). Si todavia no se pudo
 * geocodificar, cae en una posicion aproximada (PosicionesEnMapa) y el mapa avisa que lo es.
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
        Integer meses;
        String mesesParam = ctx.queryParam("meses");
        if (mesesParam != null && !mesesParam.isBlank()) {
            try {
                int parsed = Integer.parseInt(mesesParam.trim());
                meses = parsed <= 0 ? null : parsed;
            } catch (NumberFormatException ignored) {
                meses = 3;
            }
        } else {
            meses = 3; // Por defecto: ultimos 3 meses
        }

        List<EntidadConEntregas> entidades = Arrays.asList(donaciones.entregasPorEntidad(meses));
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
        modelo.put("hayAproximadas", vista.stream().anyMatch(EntidadVista::aproximada));
        modelo.put("mesesSeleccionados", meses != null ? meses : 0);
        modelo.put("esMeses3", meses != null && meses == 3);
        modelo.put("esMeses6", meses != null && meses == 6);
        modelo.put("esMeses12", meses != null && meses == 12);
        modelo.put("esMesesTodos", meses == null || meses == 0);
        ctx.render(VISTA, modelo);
    }

    /**
     * GET /direcciones/ubicar?calle=&altura=&localidad=&provincia= (publico): el formulario de registro de la
     * entidad lo llama mientras se escribe la direccion, para mostrarla en el mapa y que se verifique.
     * Responde 200 con el punto, 400 con el mensaje si no existe o falta algo, 503 si no se pudo buscar.
     */
    public void ubicarDireccion(Context ctx) {
        String calle = ctx.queryParam("calle");
        String altura = ctx.queryParam("altura");
        String localidad = ctx.queryParam("localidad");
        String provincia = ctx.queryParam("provincia");
        if (vacio(calle) || vacio(altura) || vacio(localidad) || vacio(provincia)) {
            ctx.status(400).json(Map.of("mensaje", "Completá la calle, la altura, la localidad y la provincia."));
            return;
        }
        try {
            ctx.json(donaciones.ubicarDireccion(DireccionPostal.componer(calle, altura, localidad, provincia)));
        } catch (ApiException e) {
            if (e.getStatus() == 400) {
                ctx.status(400).json(Map.of("mensaje", e.getMessage()));
            } else {
                ctx.status(503).json(Map.of("mensaje", NO_SE_PUDO_UBICAR));
            }
        } catch (RuntimeException e) {
            ctx.status(503).json(Map.of("mensaje", NO_SE_PUDO_UBICAR));
        }
    }

    private static final String NO_SE_PUDO_UBICAR =
            "No pudimos buscar la dirección en el mapa ahora. La vamos a verificar cuando te registres.";

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Un marcador y su detalle. La primera (la de la entrega mas reciente) arranca elegida. */
    public record EntidadVista(Long id, String razonSocial, String direccion, int cantidad, String etiquetaCantidad,
            double latitud, double longitud, boolean aproximada, boolean elegida, List<EntregaVista> entregas) {

        static EntidadVista de(EntidadConEntregas e, PosicionesEnMapa.Punto aproximado, boolean elegida) {
            List<EntregaVista> entregas = e.entregas().stream().map(EntregaVista::de).toList();
            int cantidad = entregas.size();
            boolean ubicada = e.latitud() != null && e.longitud() != null;
            return new EntidadVista(e.entidadId(), e.razonSocial(), e.direccion(), cantidad,
                    cantidad == 1 ? "1 donación" : cantidad + " donaciones",
                    ubicada ? e.latitud() : aproximado.latitud(), ubicada ? e.longitud() : aproximado.longitud(),
                    !ubicada, elegida, entregas);
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
