package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.ArrayList;
import java.util.List;

/**
 * El mapa de entregas es ilustrativo (los profes permitieron mockearlo): no hay coordenadas reales.
 * Cada entidad cae siempre en el mismo punto, calculado a partir de su id, dentro de la parte "de tierra"
 * del dibujo (public/img/mapa-ilustrativo.svg: el rio ocupa el 20 % derecho), arriba de la leyenda
 * y sin pisar a las demas.
 * Las posiciones son porcentajes del ancho y del alto del mapa.
 */
final class PosicionesEnMapa {

    record Punto(double x, double y) {
    }

    private static final double X_MIN = 8, X_MAX = 74, Y_MIN = 14, Y_MAX = 82;
    private static final double DISTANCIA_MINIMA = 9;
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
        return new Punto(redondear(X_MIN + a * (X_MAX - X_MIN)), redondear(Y_MIN + b * (Y_MAX - Y_MIN)));
    }

    private static boolean muyCerca(Punto punto, List<Punto> otros) {
        return otros.stream().anyMatch(o -> Math.hypot(o.x() - punto.x(), o.y() - punto.y()) < DISTANCIA_MINIMA);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
