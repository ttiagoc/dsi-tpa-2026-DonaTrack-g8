package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.ArrayList;
import java.util.List;

/**
 * Respaldo para las entidades que todavia no tienen coordenadas (Georef no respondio o la direccion es de antes de
 * que se verificaran): cada una cae siempre en el mismo punto de la Ciudad de Buenos Aires, calculado a partir de
 * su id, y sin pisar a las demas. El mapa avisa que esas ubicaciones son aproximadas.
 * El rectangulo queda lejos del Rio de la Plata y de la General Paz para que ningun marcador caiga en el agua
 * ni fuera de la ciudad. El mapa (Leaflet + OpenStreetMap) se dibuja con estas latitudes y longitudes.
 */
final class PosicionesEnMapa {

    record Punto(double latitud, double longitud) {
    }

    static final double LAT_MIN = -34.650, LAT_MAX = -34.575, LNG_MIN = -58.495, LNG_MAX = -58.410;
    /** Unos 800 m: separados alcanza para distinguirlos con el zoom que abarca a todos. */
    static final double DISTANCIA_MINIMA = 0.008;
    private static final int INTENTOS = 40;

    private PosicionesEnMapa() {
    }

    static List<Punto> ubicar(List<Long> ids) {
        List<Punto> puntos = new ArrayList<>();
        for (Long id : ids) {
            long semilla = id == null ? 0 : id;
            Punto punto = candidato(semilla, 0);
            for (int intento = 1; intento < INTENTOS && muyCerca(punto, puntos); intento++) {
                punto = candidato(semilla, intento);
            }
            puntos.add(punto);
        }
        return puntos;
    }

    // mezcla id e intento (como un hash) y reparte el resultado en el rectangulo permitido
    private static Punto candidato(long id, int intento) {
        long h = (id * 0x9E3779B97F4A7C15L) ^ (intento * 0xBF58476D1CE4E5B9L);
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        double a = (h & 0xFFFF) / 65535.0;
        double b = ((h >>> 16) & 0xFFFF) / 65535.0;
        return new Punto(redondear(LAT_MIN + a * (LAT_MAX - LAT_MIN)), redondear(LNG_MIN + b * (LNG_MAX - LNG_MIN)));
    }

    static double distancia(Punto a, Punto b) {
        return Math.hypot(a.latitud() - b.latitud(), a.longitud() - b.longitud());
    }

    private static boolean muyCerca(Punto punto, List<Punto> otros) {
        return otros.stream().anyMatch(o -> distancia(o, punto) < DISTANCIA_MINIMA);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 1_000_000) / 1_000_000.0;
    }
}
