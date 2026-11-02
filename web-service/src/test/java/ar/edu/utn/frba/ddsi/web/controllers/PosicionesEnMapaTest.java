package ar.edu.utn.frba.ddsi.web.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Posiciones de los marcadores del mapa ilustrativo")
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
    @DisplayName("Quedan en la parte de tierra del dibujo y no se pisan")
    void quedanAdentroYSeparadas() {
        List<PosicionesEnMapa.Punto> puntos = PosicionesEnMapa.ubicar(ids);
        for (PosicionesEnMapa.Punto p : puntos) {
            assertTrue(p.x() >= 8 && p.x() <= 74, "x fuera del mapa o sobre el rio: " + p);
            assertTrue(p.y() >= 14 && p.y() <= 82, "y fuera del mapa: " + p);
        }
        for (int i = 0; i < puntos.size(); i++) {
            for (int j = i + 1; j < puntos.size(); j++) {
                double distancia = Math.hypot(puntos.get(i).x() - puntos.get(j).x(), puntos.get(i).y() - puntos.get(j).y());
                assertTrue(distancia >= 9, "se pisan " + puntos.get(i) + " y " + puntos.get(j));
            }
        }
    }
}
