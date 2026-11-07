package ar.edu.utn.frba.ddsi.web.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.dto.comun.PaginaResponse;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionResponse;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.SituacionDonacion;
import io.javalin.http.Context;

/**
 * Req. 11: la entidad ve el estado de las donaciones que le asignaron. Las entregadas sin confirmar
 * llevan a confirmar la recepcion (req. 12).
 */
@Component
public class DonacionesAsignadasController {

    private static final String VISTA = "entidad/donaciones.html.hbs";
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

    private final DonacionesApiClient donaciones;

    public DonacionesAsignadasController(DonacionesApiClient donaciones) {
        this.donaciones = donaciones;
    }

    public void listar(Context ctx) {
        List<DonacionVista> todas = donaciones.listarDonacionesAsignadas(ctx).stream()
                .map(DonacionVista::de).toList();
        PaginaResponse<DonacionVista> pagina = Paginador.paginarLista(ctx, todas, 10);

        Map<String, Object> modelo = Modelo.base(ctx);
        modelo.put("donaciones", pagina.elementos());
        modelo.put("paginacion", Paginador.armarModelo(ctx, pagina, "/entidad/donaciones"));
        modelo.put("resumen", Map.of(
                "total", todas.size(),
                "porLlegar", contar(todas, null),
                "paraConfirmar", contar(todas, SituacionDonacion.PARA_CONFIRMAR),
                "recibidas", contar(todas, SituacionDonacion.RECIBIDA)));
        ctx.render(VISTA, modelo);
    }

    // sin situacion: las que todavia no llegaron
    private static long contar(List<DonacionVista> donaciones, SituacionDonacion situacion) {
        return donaciones.stream()
                .filter(d -> situacion == null ? d.situacion().porLlegar() : d.situacion() == situacion)
                .count();
    }

    /** Una fila de la tabla: los bienes como "descripcion × cantidad" y el estado con su detalle. */
    public record DonacionVista(Long id, String asignadaEl, String necesidad, String necesidadDescripcion,
            List<String> bienes, SituacionDonacion situacion, String etiqueta, String claseEtiqueta, String detalle,
            boolean puedeConfirmar) {

        static DonacionVista de(DonacionResponse d) {
            SituacionDonacion situacion = SituacionDonacion.de(d);
            List<String> bienes = d.bienes() == null ? List.of()
                    : d.bienes().stream().map(b -> b.descripcion() + " × " + b.cantidad()).toList();
            String necesidad = d.necesidad() != null ? d.necesidad().subcategoria() : d.subcategoria();
            String descripcion = d.necesidad() != null ? d.necesidad().descripcion() : null;
            return new DonacionVista(d.id(), dia(d.fechaAsignacion()), necesidad, descripcion, bienes, situacion,
                    situacion.etiqueta(), situacion.clase(), detalle(situacion, d),
                    situacion == SituacionDonacion.PARA_CONFIRMAR);
        }

        private static String detalle(SituacionDonacion situacion, DonacionResponse d) {
            String desde = dia(d.fechaEstado());
            return switch (situacion) {
                case ASIGNADA -> "LISTA_PARA_ENTREGAR".equals(d.estadoActual()) ? "Lista para salir" : "Esperando el envío";
                case EN_CAMINO -> "Desde el " + desde;
                case ENTREGA_FALLIDA -> "El " + desde + " · se reprograma";
                case PARA_CONFIRMAR -> "El " + desde + " · falta confirmar";
                case RECIBIDA -> "Entregada el " + desde + " · " + fotos(d.fotosRecepcion().size());
                case VENCIDA -> "El " + desde;
            };
        }

        private static String fotos(int cantidad) {
            return cantidad == 1 ? "1 foto" : cantidad + " fotos";
        }

        private static String dia(LocalDateTime fecha) {
            return fecha == null ? "" : fecha.format(DIA);
        }
    }
}
