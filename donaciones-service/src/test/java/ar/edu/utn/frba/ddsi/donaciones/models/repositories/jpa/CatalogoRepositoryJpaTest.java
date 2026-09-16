package ar.edu.utn.frba.ddsi.donaciones.models.repositories.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaCategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaSubcategoria;

@DisplayName("Persistencia del catalogo (Categoria y Subcategoria)")
class CatalogoRepositoryJpaTest extends PersistenciaTest {

    private final JpaCategoria categorias = new JpaCategoria();
    private final JpaSubcategoria subcategorias = new JpaSubcategoria();

    @Test
    @DisplayName("Guarda una categoria con sus flags y la busca por nombre ignorando mayusculas")
    void buscaCategoriaPorNombre() {
        Long id = categorias.save(new Categoria("Alimentos", false, true)).getId();
        nuevoRequest();

        Categoria leida = categorias.findByNombre("ALIMENTOS").orElseThrow();

        assertEquals(id, leida.getId());
        assertFalse(leida.getPideEstado());
        assertTrue(leida.getEsPerecedero());
        assertTrue(categorias.findByNombre("Bebidas").isEmpty());
    }

    @Test
    @DisplayName("La base rechaza dos categorias con el mismo nombre (UNIQUE)")
    void nombreDeCategoriaEsUnico() {
        categorias.save(new Categoria("Alimentos", false, true));
        nuevoRequest();

        assertThrows(RuntimeException.class,
                () -> categorias.save(new Categoria("Alimentos", true, false)));
        rollbackTransaction();
        nuevoRequest();

        assertEquals(1, categorias.findAll().size());
    }

    @Test
    @DisplayName("Las subcategorias se recuperan por categoria y derivan sus flags de ella")
    void subcategoriasDeUnaCategoria() {
        Categoria alimentos = categorias.save(new Categoria("Alimentos", false, true));
        Categoria mobiliario = categorias.save(new Categoria("Mobiliario", true, false));
        subcategorias.save(new Subcategoria("Fideos", alimentos));
        subcategorias.save(new Subcategoria("Arroz", alimentos));
        subcategorias.save(new Subcategoria("Sillas", mobiliario));
        nuevoRequest();

        List<Subcategoria> deAlimentos = subcategorias.findByCategoriaId(alimentos.getId());

        assertEquals(2, deAlimentos.size());
        assertTrue(deAlimentos.stream().allMatch(Subcategoria::esPerecedero));
        assertTrue(deAlimentos.stream().noneMatch(Subcategoria::pideEstado));
        assertEquals(1, subcategorias.findByCategoriaId(mobiliario.getId()).size());
    }

    @Test
    @DisplayName("La subcategoria conserva la referencia a su categoria al releerla")
    void subcategoriaConservaSuCategoria() {
        Categoria mobiliario = categorias.save(new Categoria("Mobiliario", true, false));
        Long id = subcategorias.save(new Subcategoria("Sillas", mobiliario)).getId();
        nuevoRequest();

        Subcategoria porId = subcategorias.findById(id).orElseThrow();
        assertEquals("Mobiliario", porId.getCategoria().getNombre());
        assertTrue(porId.pideEstado());
        assertFalse(porId.esPerecedero());

        Subcategoria porNombre = subcategorias.findByNombre("sillas").orElseThrow();
        assertEquals(id, porNombre.getId());
    }
}
