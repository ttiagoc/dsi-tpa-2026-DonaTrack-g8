package ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Consultas que se le hacen a Georef segun como se escribio la direccion")
class GeorefGeocodificadorTest {

    @Test
    @DisplayName("Con calle, barrio y provincia: primero filtra por los dos y despues solo por la provincia")
    void calleBarrioYProvincia() {
        List<Map<String, String>> consultas = GeorefGeocodificador.consultas("Calle Lavalleja 850, Villa Crespo, CABA");

        assertEquals(Map.of("direccion", "Lavalleja 850", "provincia", "CABA", "localidad", "Villa Crespo"), consultas.get(0),
                "el \"Calle\" de adelante se saca: Georef no la encuentra con esa palabra");
        assertEquals(Map.of("direccion", "Lavalleja 850", "provincia", "CABA", "departamento", "Villa Crespo"), consultas.get(1));
        assertEquals(Map.of("direccion", "Lavalleja 850", "provincia", "CABA"), consultas.get(2));
    }

    @Test
    @DisplayName("Con una sola parte despues de la calle, se prueba como provincia, localidad y partido")
    void calleYLugar() {
        List<Map<String, String>> consultas = GeorefGeocodificador.consultas(" Av. San Juan 3100 ,  CABA ");

        assertEquals(List.of(
                Map.of("direccion", "Av. San Juan 3100", "provincia", "CABA"),
                Map.of("direccion", "Av. San Juan 3100", "localidad", "CABA"),
                Map.of("direccion", "Av. San Juan 3100", "departamento", "CABA")), consultas);
    }

    @Test
    @DisplayName("Sin altura o sin localidad la direccion esta incompleta")
    void incompletas() {
        assertThrows(DireccionInvalidaException.class, () -> GeorefGeocodificador.consultas("Av. Eva Peron 4200"));
        assertThrows(DireccionInvalidaException.class, () -> GeorefGeocodificador.consultas("Av. Eva Peron, CABA"));
        assertThrows(DireccionInvalidaException.class, () -> GeorefGeocodificador.consultas(""));
    }
}
