package ar.edu.utn.frba.ddsi.web.server.templates;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;

public class ComponentesTemplateTest {

    private JavalinHandlebars renderer;
    private Handlebars handlebars;

    @BeforeEach
    void setUp() {
        renderer = new JavalinHandlebars();
        handlebars = renderer.getHandlebars();
    }

    private String renderString(String templateStr, Map<String, ?> contextMap) throws IOException {
        Template template = handlebars.compileInline(templateStr);
        com.github.jknack.handlebars.Context context = com.github.jknack.handlebars.Context.newBuilder(contextMap)
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, MethodValueResolver.INSTANCE)
                .build();
        return template.apply(context);
    }

    @Test
    void testBotonHelperAlta() throws IOException {
        String result = renderString("{{{boton 'alta' href='/admin/camiones?nuevo' texto='+ Nuevo camión' modal='modal-camion'}}}", Map.of());
        assertTrue(result.contains("<a class=\"boton\" href=\"/admin/camiones?nuevo\" data-abre=\"modal-camion\">"));
        assertTrue(result.contains("#i-mas"));
        // El helper limpia el '+' redundante si ya lleva el icono de alta i-mas
        assertTrue(result.contains("Nuevo camión"));
        assertFalse(result.contains("+ Nuevo camión"));

        String resultDefault = renderString("{{{boton 'alta'}}}", Map.of());
        assertTrue(resultDefault.contains("Nuevo"));
        assertFalse(resultDefault.contains("+ Nuevo"));
    }

    @Test
    void testBotonHelperEditarYEliminarConSrOnly() throws IOException {
        String resultEditar = renderString("{{{boton 'editar' href='/admin/camiones?editar=12' sr='camión AB123CD'}}}", Map.of());
        assertTrue(resultEditar.contains("class=\"boton boton--secundario boton--chico\""));
        assertTrue(resultEditar.contains("#i-lapiz"));
        assertTrue(resultEditar.contains("Editar"));
        assertTrue(resultEditar.contains("<span class=\"sr-only\">camión AB123CD</span>"));

        String resultEliminar = renderString("{{{boton 'eliminar' href='/admin/camiones?eliminar=12' sr='camión AB123CD'}}}", Map.of());
        assertTrue(resultEliminar.contains("class=\"boton boton--secundario boton--peligro boton--chico\""));
        assertTrue(resultEliminar.contains("#i-basura"));
        assertTrue(resultEliminar.contains("Eliminar"));
        assertTrue(resultEliminar.contains("<span class=\"sr-only\">camión AB123CD</span>"));
    }

    @Test
    void testBotonHelperConfirmarPeligroSubmit() throws IOException {
        String result = renderString("{{{boton 'confirmar_peligro' texto='Eliminar definitivamente' type='submit'}}}", Map.of());
        assertTrue(result.contains("<button type=\"submit\" class=\"boton boton--peligro\">"));
        assertTrue(result.contains("Eliminar definitivamente"));
    }

    @Test
    void testEstadoHelper() throws IOException {
        String enDeposito = renderString("{{{estado 'EN_DEPOSITO'}}}", Map.of());
        assertTrue(enDeposito.contains("class=\"etiqueta\""));
        assertTrue(enDeposito.contains("En depósito"));

        String asignada = renderString("{{{estado 'ASIGNACION_REALIZADA'}}}", Map.of());
        assertTrue(asignada.contains("class=\"etiqueta etiqueta--info\""));
        assertTrue(asignada.contains("Asignación realizada"));

        String enTraslado = renderString("{{{estado 'EN_TRASLADO'}}}", Map.of());
        assertTrue(enTraslado.contains("class=\"etiqueta etiqueta--pendiente\""));
        assertTrue(enTraslado.contains("En traslado"));

        String recibida = renderString("{{{estado 'RECIBIDA'}}}", Map.of());
        assertTrue(recibida.contains("class=\"etiqueta etiqueta--ok\""));
        assertTrue(recibida.contains("Recibida"));

        String vencida = renderString("{{{estado 'VENCIDA'}}}", Map.of());
        assertTrue(vencida.contains("class=\"etiqueta etiqueta--error\""));
        assertTrue(vencida.contains("Vencida"));
    }

    @Test
    void testFechaHelper() throws IOException {
        LocalDate fecha = LocalDate.of(2026, 11, 4);
        String formatted = renderString("{{fecha fecha}}", Map.of("fecha", fecha));
        assertTrue(formatted.contains("04/11/2026"));

        LocalDateTime fechaHora = LocalDateTime.of(2026, 11, 4, 15, 30);
        String formattedHora = renderString("{{fecha fechaHora hora=true}}", Map.of("fechaHora", fechaHora));
        assertTrue(formattedHora.contains("04/11/2026 15:30"));
    }

    @Test
    void testConcatHelper() throws IOException {
        String result = renderString("{{concat '/admin/camiones?editar=' id}}", Map.of("id", 42));
        assertTrue(result.contains("/admin/camiones?editar=42"));
    }

    @Test
    void testPartialEncabezado() throws IOException {
        String template = "{{> partials/encabezado titulo='Flota' bajada='Gestión de camiones' accionHref='/admin/camiones?nuevo' accionTexto='Nuevo camión' accionModal='modal-camion'}}";
        String result = renderString(template, Map.of());
        assertTrue(result.contains("<header class=\"encabezado-pagina\">"));
        assertTrue(result.contains("<h1>Flota</h1>"));
        assertTrue(result.contains("Gestión de camiones"));
        assertTrue(result.contains("Nuevo camión"));
        assertFalse(result.contains("+ Nuevo camión"));
        assertTrue(result.contains("data-abre=\"modal-camion\""));
    }

    @Test
    void testPartialListadoConDatosYVacio() throws IOException {
        String template = "{{#> partials/listado coleccion=items vacioTitulo='Sin elementos' vacioBajada='Creá uno nuevo'}}<table><tbody>{{#each items}}<tr><td>{{this}}</td></tr>{{/each}}</tbody></table>{{/partials/listado}}";

        String conDatos = renderString(template, Map.of("items", List.of("Camion 1", "Camion 2")));
        assertTrue(conDatos.contains("tarjeta--tabla"));
        assertTrue(conDatos.contains("Camion 1"));
        assertFalse(conDatos.contains("Sin elementos"));

        String vacio = renderString(template, Map.of("items", Collections.emptyList()));
        assertTrue(vacio.contains("tarjeta vacio"));
        assertTrue(vacio.contains("Sin elementos"));
        assertTrue(vacio.contains("Creá uno nuevo"));
        assertFalse(vacio.contains("<table>"));
    }

    @Test
    void testPartialModalConfirmar() throws IOException {
        String template = "{{> partials/modal-confirmar abrir=true id='modal-eliminar' action='/admin/camiones/5/eliminar' titulo='¿Eliminar camión?' mensaje='Se borrará el camión' detalle='No reversible' cancelarHref='/admin/camiones' confirmarTexto='Eliminar camión'}}";
        String result = renderString(template, Map.of());
        assertTrue(result.contains("<dialog class=\"modal\" id=\"modal-eliminar\""));
        assertTrue(result.contains("action=\"/admin/camiones/5/eliminar\""));
        assertTrue(result.contains("¿Eliminar camión?"));
        assertTrue(result.contains("Se borrará el camión"));
        assertTrue(result.contains("Eliminar camión"));
    }
}
