package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Categorización de Bienes y generación de keys")
class BienTest {

    @Nested
    @DisplayName("Subcategoría hereda propiedades de Categoría")
    class SubcategoriaTests {

        @Test
        @DisplayName("Una subcategoría de categoría perecedera es perecedera")
        void subcategoriaEsPerecedera() {
            Categoria cat = new Categoria("Alimentos", false, true, null);
            Subcategoria sub = new Subcategoria("Fideos", cat);

            assertTrue(sub.esPerecedero());
            assertFalse(sub.pideEstado());
        }

        @Test
        @DisplayName("Una subcategoría de categoría que pide estado, pide estado")
        void subcategoriaPideEstado() {
            Categoria cat = new Categoria("Muebles", true, false, null);
            Subcategoria sub = new Subcategoria("Sillas", cat);

            assertFalse(sub.esPerecedero());
            assertTrue(sub.pideEstado());
        }

        @Test
        @DisplayName("Subcategoría sin categoría asignada retorna false para ambas propiedades")
        void sinCategoriaRetornaFalse() {
            Subcategoria sub = new Subcategoria("Huérfana", null);

            assertFalse(sub.esPerecedero());
            assertFalse(sub.pideEstado());
        }
    }

    @Nested
    @DisplayName("Generación de key para segmentación")
    class GenerarKeyTests {

        private Categoria categoriaAlimentos;
        private Categoria categoriaMuebles;

        @BeforeEach
        void setUp() {
            categoriaAlimentos = new Categoria("Alimentos", false, true, null);
            categoriaMuebles = new Categoria("Muebles", true, false, null);
        }

        @Test
        @DisplayName("Key base es el nombre de la subcategoría")
        void keyBaseEsNombreSubcategoria() {
            Categoria catSimple = new Categoria("Servicios", false, false, null);
            Subcategoria sub = new Subcategoria("Clases", catSimple);
            Bien bien = new Bien(null, null, null, null, sub, null, null);

            assertEquals("Clases", bien.generarKey());
        }

        @Test
        @DisplayName("Perecedero incluye fecha de vencimiento en la key")
        void perecederoIncluyeVencimiento() {
            Subcategoria sub = new Subcategoria("Fideos", categoriaAlimentos);

            Bien bien = new Bien(null, null, null, null, sub, null, null);
            bien.setFechaVencimiento(LocalDate.of(2026, 12, 1));

            assertEquals("Fideos-2026-12-01", bien.generarKey());
        }

        @Test
        @DisplayName("Perecedero sin fecha de vencimiento NO incluye vencimiento")
        void perecederoSinFechaSoloNombre() {
            Subcategoria sub = new Subcategoria("Fideos", categoriaAlimentos);
            Bien bien = new Bien(null, null, null, null, sub, null, null);
            bien.setFechaVencimiento(null);

            assertEquals("Fideos", bien.generarKey());
        }

        @Test
        @DisplayName("Categoría que pide estado incluye el EstadoBien en la key")
        void pideEstadoIncluyeEstado() {
            Subcategoria sub = new Subcategoria("Sillas", categoriaMuebles);
            Bien bien = new Bien(null, null, null, null, sub, EstadoBien.USADO, null);
            bien.setEstadoBien(EstadoBien.USADO);

            assertEquals("Sillas-USADO", bien.generarKey());
        }

        @Test
        @DisplayName("Categoría que pide estado pero bien sin estado asignado NO incluye estado")
        void pideEstadoPeroSinEstado() {
            Subcategoria sub = new Subcategoria("Sillas", categoriaMuebles);
            Bien bien = new Bien(null, null, null, null, sub, null, null);
            bien.setEstadoBien(null);

            assertEquals("Sillas", bien.generarKey());
        }

        @Test
        @DisplayName("Dos bienes iguales generan la misma key")
        void dosIgualesGeneranMismaKey() {
            Subcategoria sub = new Subcategoria("Fideos", categoriaAlimentos);
            LocalDate vencimiento = LocalDate.of(2026, 12, 1);

            Bien bien1 = new Bien(null, null, null, null, sub, null, vencimiento);
            Bien bien2 = new Bien(null, null, null, null, sub, null, vencimiento);

            assertEquals(bien1.generarKey(), bien2.generarKey());
        }

        @Test
        @DisplayName("Dos bienes con distinto vencimiento generan diferente key (perecederos)")
        void distintoVencimientoDistintaKey() {
            Subcategoria sub = new Subcategoria("Leche", categoriaAlimentos);

            LocalDate vencimiento1 = LocalDate.of(2026, 6, 1);
            Bien bien1 = new Bien(null, null, null, null, sub, null, vencimiento1);

            LocalDate vencimiento2 = LocalDate.of(2026, 12, 1);
            Bien bien2 = new Bien(null, null, null, null, sub, null, vencimiento2);

            assertNotEquals(bien1.generarKey(), bien2.generarKey());
        }

        @Test
        @DisplayName("Dos bienes con distinto estado generan diferente key (pide estado)")
        void distintoEstadoDistintaKey() {
            Subcategoria sub = new Subcategoria("Sillas", categoriaMuebles);

            Bien bien1 = new Bien(null, null, null, null, sub, EstadoBien.NUEVO, null);

            Bien bien2 = new Bien(null, null, null, null, sub, EstadoBien.USADO, null);

            assertNotEquals(bien1.generarKey(), bien2.generarKey());
        }
    }
}
