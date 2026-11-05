package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * Resuelve coordenadas geograficas aproximadas dentro de CABA para el deposito central
 * y las paradas de las hojas de ruta sin depender de geocoders externos con cuota o red.
 */
public final class GeocodificadorCaba {

    public static final double DEPOSITO_LAT = -34.5986;
    public static final double DEPOSITO_LNG = -58.4202;
    public static final String DEPOSITO_DIRECCION = "Av. Medrano 951, Almagro, CABA";

    private static final double LAT_MIN = -34.6480;
    private static final double LAT_MAX = -34.5820;
    private static final double LNG_MIN = -58.4820;
    private static final double LNG_MAX = -58.3950;

    private GeocodificadorCaba() {
    }

    public static double[] ubicar(String direccion, Long entidadId) {
        String norm = normalizar(direccion);

        if (norm.contains("flores") || norm.contains("rivadavia 65")) {
            return new double[] { -34.6285, -58.4622 };
        }
        if (norm.contains("parque avellaneda") || norm.contains("eva peron")) {
            return new double[] { -34.6455, -58.4785 };
        }
        if (norm.contains("san cristobal") || norm.contains("san juan")) {
            return new double[] { -34.6238, -58.4095 };
        }
        if (norm.contains("villa crespo") || norm.contains("lavalleja")) {
            return new double[] { -34.5968, -58.4368 };
        }
        if (norm.contains("belgrano") || norm.contains("cabildo")) {
            return new double[] { -34.5620, -58.4565 };
        }
        if (norm.contains("medrano 951")) {
            return new double[] { DEPOSITO_LAT, DEPOSITO_LNG };
        }
        if (norm.contains("almagro") || norm.contains("corrientes")) {
            return new double[] { -34.6035, -58.4215 };
        }
        if (norm.contains("palermo")) {
            return new double[] { -34.5885, -58.4305 };
        }
        if (norm.contains("caballito")) {
            return new double[] { -34.6185, -58.4425 };
        }
        if (norm.contains("boedo")) {
            return new double[] { -34.6295, -58.4175 };
        }
        if (norm.contains("barracas")) {
            return new double[] { -34.6415, -58.3765 };
        }

        int h1 = Math.floorMod(Objects.hash(norm, entidadId, 17), 1000);
        int h2 = Math.floorMod(Objects.hash(entidadId, norm, 31), 1000);
        double lat = LAT_MIN + (h1 / 1000.0) * (LAT_MAX - LAT_MIN);
        double lng = LNG_MIN + (h2 / 1000.0) * (LNG_MAX - LNG_MIN);
        return new double[] { redondear(lat), redondear(lng) };
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10000.0) / 10000.0;
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }
}

