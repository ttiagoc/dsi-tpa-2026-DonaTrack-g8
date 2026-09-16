package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoBien;

@DisplayName("Tests del SegmentadorDeDonacion")
class SegmentadorDeDonacionTest {

    @Test
    @DisplayName("Cada donacion generada pertenece al registro y copia su donante y fecha")
    void segmentarPropagaRegistroDonanteYFecha() {
        Subcategoria sillas = new Subcategoria("Sillas", new Categoria("Mobiliario", true, false));
        Subcategoria mesas = new Subcategoria("Mesas", new Categoria("Mobiliario", true, false));
        PersonaHumana donante = new PersonaHumana();
        donante.setId(1L);

        RegistroDonacion registro = new RegistroDonacion(donante, "Muebles de oficina");
        List<Bien> bienes = List.of(
                new Bien("Silla", 6L, 5.0, 0.3, sillas, EstadoBien.USADO, null),
                new Bien("Mesa", 1L, 30.0, 1.5, mesas, EstadoBien.USADO, null));

        List<Donacion> donaciones = new SegmentadorDeDonacion().segmentarDonacion(registro, bienes);

        assertEquals(2, donaciones.size());
        for (Donacion donacion : donaciones) {
            assertSame(registro, donacion.getRegistroDonacion());
            assertSame(donante, donacion.getDonante());
            assertEquals(registro.getFecha(), donacion.getFecha());
        }
    }
}
