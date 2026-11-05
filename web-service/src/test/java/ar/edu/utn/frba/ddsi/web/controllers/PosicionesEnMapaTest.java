package ar.edu.utn.frba.ddsi.web.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Ubicaciones de las entidades en el mapa de entregas")
class PosicionesEnMapaTest {

    private final List<Long> ids = LongStream.rangeClosed(1, 15).boxed().toList();

    @Test
    @DisplayName("Cada entidad cae siempre en el mismo punto")
    void sonDeterministas() {
        assertEquals(PosicionesEnMapa.ubicar(ids), PosicionesEnMapa.ubicar(ids));
        assertEquals(PosicionesEnMapa.ubicar(List.of(7L)).get(0), PosicionesEnMapa.ubicar(ids).get(6),
                "sin vecinos cerca, el punto no depende de las demas entidades");
    }

    @Test
    @DisplayName("Quedan dentro de la Ciudad de Buenos Aires y no se pisan")
    void quedanAdentroYSeparadas() {
        List<PosicionesEnMapa.Punto> puntos = PosicionesEnMapa.ubicar(ids);
        for (PosicionesEnMapa.Punto p : puntos) {
            assertTrue(p.latitud() >= PosicionesEnMapa.LAT_MIN && p.latitud() <= PosicionesEnMapa.LAT_MAX,
                    "latitud fuera de la ciudad: " + p);
            assertTrue(p.longitud() >= PosicionesEnMapa.LNG_MIN && p.longitud() <= PosicionesEnMapa.LNG_MAX,
                    "longitud fuera de la ciudad: " + p);
        }
        for (int i = 0; i < puntos.size(); i++) {
            for (int j = i + 1; j < puntos.size(); j++) {
                assertTrue(PosicionesEnMapa.distancia(puntos.get(i), puntos.get(j)) >= PosicionesEnMapa.DISTANCIA_MINIMA,
                        "se pisan " + puntos.get(i) + " y " + puntos.get(j));
            }
        }
    }
}
