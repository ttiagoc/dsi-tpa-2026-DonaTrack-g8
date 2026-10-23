package ar.edu.utn.frba.ddsi.web.server.templates;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;

/**
 * Elige el motor de plantillas segun la extension del archivo (ctx.render("x.html.hbs", modelo) -> "hbs").
 * Tomado del ejemplo de la catedra (dds-utn/javalin-web-proof-of-concept, rama example-templating).
 */
public class JavalinRenderer implements FileRenderer {

    private final Map<String, FileRenderer> renderers = new HashMap<>();

    public JavalinRenderer register(String extension, FileRenderer renderer) {
        renderers.put(extension, renderer);
        return this;
    }

    @NotNull
    @Override
    public String render(@NotNull String filePath, @NotNull Map<String, ?> model, @NotNull Context context) {
        String extension = filePath.substring(filePath.lastIndexOf('.') + 1);
        FileRenderer renderer = renderers.get(extension);
        if (renderer == null) {
            throw new IllegalArgumentException("No hay un motor de plantillas registrado para ." + extension);
        }
        return renderer.render(filePath, model, context);
    }
}
