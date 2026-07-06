package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;

@DisplayName("Tests de la entidad Bien")
class BienTest {

    @Nested
    @DisplayName("Lógica de herencia de propiedades (Categoría -> Subcategoría -> Bien)")
    class PropiedadesTests {

        @Test
        @DisplayName("Una subcategoría hereda el atributo esPerecedero de su categoría padre")
        void subcategoriaHeredaPerecedero() {
            Categoria catAlimentos = new Categoria("Alimentos", false, true);
            Subcategoria subFideos = new Subcategoria("Fideos", catAlimentos);

            assertTrue(subFideos.esPerecedero());
            assertFalse(subFideos.pideEstado());
        }

        @Test
        @DisplayName("Una subcategoría hereda el atributo pideEstado de su categoría padre")
        void subcategoriaHeredaPideEstado() {
            Categoria catMuebles = new Categoria("Muebles", true, false);
            Subcategoria subSillas = new Subcategoria("Sillas", catMuebles);

            assertTrue(subSillas.pideEstado());
            assertFalse(subSillas.esPerecedero());
        }
    }

    @Nested
    @DisplayName("Validaciones al instanciar un Bien")
    class ValidacionesBienTests {

        @Test
        @DisplayName("Debe permitir instanciar un bien válido")
        void bienValido() {
            Categoria catMuebles = new Categoria("Muebles", true, false);
            Subcategoria subSillas = new Subcategoria("Sillas", catMuebles);

            Bien silla = new Bien("Silla de madera", 4L, 1.0, 1.0, subSillas, EstadoBien.NUEVO, null);

            assertEquals("Silla de madera", silla.getDescripcion());
            assertEquals(EstadoBien.NUEVO, silla.getEstadoBien());
            assertNull(silla.getFechaVencimiento());
        }

        @Test
        @DisplayName("Debe exigir fecha de vencimiento si es perecedero")
        void bienPerecederoExigeVencimiento() {
            Categoria catAlimentos = new Categoria("Alimentos", false, true);
            Subcategoria subFideos = new Subcategoria("Fideos", catAlimentos);

            Exception ex = assertThrows(BusinessException.class, () -> {
                new Bien("Fideos tirabuzón", 10L, 1.0, 1.0, subFideos, null, null);
            });

            assertTrue(ex.getMessage().contains("fecha de vencimiento"));
        }

        @Test
        @DisplayName("Debe exigir estado si la categoría pide estado")
        void bienConEstadoExigeEstado() {
            Categoria catMuebles = new Categoria("Muebles", true, false);
            Subcategoria subSillas = new Subcategoria("Sillas", catMuebles);

            Exception ex = assertThrows(BusinessException.class, () -> {
                new Bien("Silla de madera", 4L, 1.0, 1.0, subSillas, null, null);
            });

            assertTrue(ex.getMessage().contains("estado"));
        }
    }
}
