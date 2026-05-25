package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para el Requerimiento: "Categorización de bienes"
 * Los bienes pertenecen a una Subcategoría que a su vez pertenece a una Categoría.
 * Las categorías definen si los bienes "piden estado" (NUEVO/USADO) y si son "perecederos"
 * (con fecha de vencimiento). La key generada por un Bien se usa para la segmentación.
 */
@DisplayName("Categorización de Bienes y generación de keys")
class BienTest {

    @Nested
    @DisplayName("Subcategoría hereda propiedades de Categoría")
    class SubcategoriaTests {

        @Test
        @DisplayName("Una subcategoría de categoría perecedera es perecedera")
        void subcategoriaEsPerecedera() {
            Categoria cat = new Categoria();
            cat.setNombre("Alimentos");
            cat.setEsPerecedero(true);
            cat.setPideEstado(false);

            Subcategoria sub = new Subcategoria();
            sub.setNombre("Fideos");
            sub.setCategoria(cat);

            assertTrue(sub.esPerecedero());
            assertFalse(sub.pideEstado());
        }

        @Test
        @DisplayName("Una subcategoría de categoría que pide estado, pide estado")
        void subcategoriaPideEstado() {
            Categoria cat = new Categoria();
            cat.setNombre("Muebles");
            cat.setEsPerecedero(false);
            cat.setPideEstado(true);

            Subcategoria sub = new Subcategoria();
            sub.setNombre("Sillas");
            sub.setCategoria(cat);

            assertFalse(sub.esPerecedero());
            assertTrue(sub.pideEstado());
        }

        @Test
        @DisplayName("Subcategoría sin categoría asignada retorna false para ambas propiedades")
        void sinCategoriaRetornaFalse() {
            Subcategoria sub = new Subcategoria();
            sub.setNombre("Huérfana");

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
            categoriaAlimentos = new Categoria();
            categoriaAlimentos.setNombre("Alimentos");
            categoriaAlimentos.setEsPerecedero(true);
            categoriaAlimentos.setPideEstado(false);

            categoriaMuebles = new Categoria();
            categoriaMuebles.setNombre("Muebles");
            categoriaMuebles.setEsPerecedero(false);
            categoriaMuebles.setPideEstado(true);
        }

        @Test
        @DisplayName("Key base es el nombre de la subcategoría")
        void keyBaseEsNombreSubcategoria() {
            Categoria catSimple = new Categoria();
            catSimple.setNombre("Servicios");
            catSimple.setEsPerecedero(false);
            catSimple.setPideEstado(false);

            Subcategoria sub = new Subcategoria();
            sub.setNombre("Clases");
            sub.setCategoria(catSimple);

            Bien bien = new Bien();
            bien.setSubcategoria(sub);

            assertEquals("Clases", bien.generarKey());
        }

        @Test
        @DisplayName("Perecedero incluye fecha de vencimiento en la key")
        void perecederoIncluyeVencimiento() {
            Subcategoria sub = new Subcategoria();
            sub.setNombre("Fideos");
            sub.setCategoria(categoriaAlimentos);

            Bien bien = new Bien();
            bien.setSubcategoria(sub);
            bien.setFechaVencimiento(LocalDate.of(2026, 12, 1));

            assertEquals("Fideos-2026-12-01", bien.generarKey());
        }

        @Test
        @DisplayName("Perecedero sin fecha de vencimiento NO incluye vencimiento")
        void perecederoSinFechaSoloNombre() {
            Subcategoria sub = new Subcategoria();
            sub.setNombre("Fideos");
            sub.setCategoria(categoriaAlimentos);

            Bien bien = new Bien();
            bien.setSubcategoria(sub);
            bien.setFechaVencimiento(null);

            assertEquals("Fideos", bien.generarKey());
        }

        @Test
        @DisplayName("Categoría que pide estado incluye el EstadoBien en la key")
        void pideEstadoIncluyeEstado() {
            Subcategoria sub = new Subcategoria();
            sub.setNombre("Sillas");
            sub.setCategoria(categoriaMuebles);

            Bien bien = new Bien();
            bien.setSubcategoria(sub);
            bien.setEstadoBien(EstadoBien.USADO);

            assertEquals("Sillas-USADO", bien.generarKey());
        }

        @Test
        @DisplayName("Categoría que pide estado pero bien sin estado asignado NO incluye estado")
        void pideEstadoPeroSinEstado() {
            Subcategoria sub = new Subcategoria();
            sub.setNombre("Sillas");
            sub.setCategoria(categoriaMuebles);

            Bien bien = new Bien();
            bien.setSubcategoria(sub);
            bien.setEstadoBien(null);

            assertEquals("Sillas", bien.generarKey());
        }

        @Test
        @DisplayName("Dos bienes iguales generan la misma key")
        void dosIgualesGeneranMismaKey() {
            Subcategoria sub = new Subcategoria();
            sub.setNombre("Fideos");
            sub.setCategoria(categoriaAlimentos);

            LocalDate vencimiento = LocalDate.of(2026, 12, 1);

            Bien bien1 = new Bien();
            bien1.setSubcategoria(sub);
            bien1.setFechaVencimiento(vencimiento);

            Bien bien2 = new Bien();
            bien2.setSubcategoria(sub);
            bien2.setFechaVencimiento(vencimiento);

            assertEquals(bien1.generarKey(), bien2.generarKey());
        }

        @Test
        @DisplayName("Dos bienes con distinto vencimiento generan diferente key (perecederos)")
        void distintoVencimientoDistintaKey() {
            Subcategoria sub = new Subcategoria();
            sub.setNombre("Leche");
            sub.setCategoria(categoriaAlimentos);

            Bien bien1 = new Bien();
            bien1.setSubcategoria(sub);
            bien1.setFechaVencimiento(LocalDate.of(2026, 6, 1));

            Bien bien2 = new Bien();
            bien2.setSubcategoria(sub);
            bien2.setFechaVencimiento(LocalDate.of(2026, 12, 1));

            assertNotEquals(bien1.generarKey(), bien2.generarKey());
        }

        @Test
        @DisplayName("Dos bienes con distinto estado generan diferente key (pide estado)")
        void distintoEstadoDistintaKey() {
            Subcategoria sub = new Subcategoria();
            sub.setNombre("Sillas");
            sub.setCategoria(categoriaMuebles);

            Bien bien1 = new Bien();
            bien1.setSubcategoria(sub);
            bien1.setEstadoBien(EstadoBien.NUEVO);

            Bien bien2 = new Bien();
            bien2.setSubcategoria(sub);
            bien2.setEstadoBien(EstadoBien.USADO);

            assertNotEquals(bien1.generarKey(), bien2.generarKey());
        }
    }
}
