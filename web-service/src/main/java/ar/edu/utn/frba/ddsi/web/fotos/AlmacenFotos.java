package ar.edu.utn.frba.ddsi.web.fotos;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.config.WebConfig;
import io.javalin.http.UploadedFile;

/**
 * Guarda las fotos que suben los usuarios en el disco de la maquina (carpeta FOTOS_DIR) y el Server
 * las publica en /fotos/<archivo>, sin login (el mapa publico las muestra).
 * El nombre lo elige el servidor (uuid) y solo se aceptan JPG y PNG reales: se mira el contenido, no el nombre.
 * En la nube la carpeta tiene que estar en un disco persistente; el del contenedor se borra en cada deploy.
 */
@Component
public class AlmacenFotos {

    public static final String RUTA_PUBLICA = "/fotos";
    public static final int MAXIMO_FOTOS = 5;
    public static final int MAXIMO_MB = 5;

    private final Path directorio;

    public AlmacenFotos(WebConfig config) {
        this.directorio = Path.of(config.getFotos().getDirectorio()).toAbsolutePath();
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de fotos " + directorio, e);
        }
    }

    public Path getDirectorio() {
        return directorio;
    }

    /** null si la foto es valida; si no, el mensaje para el formulario. */
    public String validar(UploadedFile foto) {
        if (foto.size() > MAXIMO_MB * 1024L * 1024L) {
            return "\"" + foto.filename() + "\" pesa más de " + MAXIMO_MB + " MB";
        }
        if (formatoDe(foto).isEmpty()) {
            return "\"" + foto.filename() + "\" no es una foto JPG o PNG";
        }
        return null;
    }

    /** Guarda una foto ya validada y devuelve la URL con la que se publica. */
    public String guardar(UploadedFile foto) {
        Formato formato = formatoDe(foto).orElseThrow(() -> new IllegalArgumentException("Formato no aceptado"));
        String nombre = UUID.randomUUID() + "." + formato.extension;
        try (InputStream contenido = foto.content()) {
            Files.copy(contenido, directorio.resolve(nombre));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto", e);
        }
        return RUTA_PUBLICA + "/" + nombre;
    }

    /** Borra fotos guardadas (por ejemplo, si la API rechazo la confirmacion). */
    public void borrar(List<String> urls) {
        for (String url : urls) {
            String nombre = url.substring(url.lastIndexOf('/') + 1);
            try {
                Files.deleteIfExists(directorio.resolve(nombre));
            } catch (IOException e) {
                System.err.println("No se pudo borrar la foto " + nombre + ": " + e.getMessage());
            }
        }
    }

    private Optional<Formato> formatoDe(UploadedFile foto) {
        byte[] inicio;
        try (InputStream contenido = foto.content()) {
            inicio = contenido.readNBytes(8);
        } catch (IOException e) {
            return Optional.empty();
        }
        return Arrays.stream(Formato.values()).filter(f -> f.coincide(inicio)).findFirst();
    }

    /** Los primeros bytes de cada formato aceptado. */
    private enum Formato {
        JPG("jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        PNG("png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

        private final String extension;
        private final byte[] firma;

        Formato(String extension, byte[] firma) {
            this.extension = extension;
            this.firma = firma;
        }

        boolean coincide(byte[] inicio) {
            return inicio.length >= firma.length && Arrays.equals(Arrays.copyOf(inicio, firma.length), firma);
        }
    }
}
