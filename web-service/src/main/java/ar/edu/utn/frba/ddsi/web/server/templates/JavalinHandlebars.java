package ar.edu.utn.frba.ddsi.web.server.templates;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;

/**
 * Renderiza las plantillas Handlebars de src/main/resources/templates (basado en el ejemplo de la catedra).
 * Cambios respecto del original:
 * - el loader ya conoce la carpeta y la extension, asi los partials se nombran igual que las vistas
 *   ({{> layouts/base}} -> templates/layouts/base.html.hbs);
 * - MethodValueResolver para leer records ({{usuario.username}} llama a username());
 * - los helpers de ConditionalHelpers (eq, neq, and, or, not) para las vistas;
 * - una plantilla que no existe es un error de programacion: se propaga y termina en la pagina 500.
 */
public class JavalinHandlebars implements FileRenderer {

    private static final String EXTENSION = ".html.hbs";

    private final Handlebars handlebars;

    public JavalinHandlebars() {
        this.handlebars = new Handlebars(new ClassPathTemplateLoader("/templates", EXTENSION))
                .with(new ConcurrentMapTemplateCache());
        this.handlebars.registerHelpers(ConditionalHelpers.class);
    }

    @NotNull
    @Override
    public String render(@NotNull String filePath, @NotNull Map<String, ?> model, @NotNull Context context) {
        try {
            Template template = handlebars.compile(filePath.replace(EXTENSION, ""));
            com.github.jknack.handlebars.Context contexto = com.github.jknack.handlebars.Context.newBuilder(model)
                    .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE,
                            MethodValueResolver.INSTANCE)
                    .build();
            return template.apply(contexto);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo renderizar la plantilla " + filePath, e);
        }
    }
}
