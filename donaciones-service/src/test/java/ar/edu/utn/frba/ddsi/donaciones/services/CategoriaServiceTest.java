package ar.edu.utn.frba.ddsi.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.UsosCatalogoResponse;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.CategoriaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.SubcategoriaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.impl.CategoriaServiceImpl;

@DisplayName("Tests del CategoriaService")
class CategoriaServiceTest {

    private CategoriaRepository categoriaRepository;
    private SubcategoriaRepository subcategoriaRepository;
    private CategoriaServiceImpl service;

    private Categoria alimentos;
    private Subcategoria arroz;

    @BeforeEach
    void setUp() {
        categoriaRepository = mock(CategoriaRepository.class);
        subcategoriaRepository = mock(SubcategoriaRepository.class);
        service = new CategoriaServiceImpl(categoriaRepository, subcategoriaRepository);

        alimentos = new Categoria(1L, "Alimentos", false, true);
        arroz = new Subcategoria(10L, "Arroz", alimentos);

        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subcategoriaRepository.save(any(Subcategoria.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Actualiza nombre, estado y perecedero de una categoria")
    void actualizaCategoria() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(alimentos));
        when(categoriaRepository.findByNombre("Comestibles")).thenReturn(Optional.empty());

        CategoriaResponse respuesta = service.actualizar(1L, new CategoriaRequest("  Comestibles ", true, false));

        assertEquals("Comestibles", respuesta.nombre());
        assertTrue(respuesta.pideEstado());
        assertFalse(respuesta.esPerecedero());
    }

    @Test
    @DisplayName("Puede guardar una categoria con su mismo nombre")
    void actualizaCategoriaConSuMismoNombre() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(alimentos));
        when(categoriaRepository.findByNombre("Alimentos")).thenReturn(Optional.of(alimentos));

        CategoriaResponse respuesta = service.actualizar(1L, new CategoriaRequest("Alimentos", true, true));

        assertTrue(respuesta.pideEstado());
    }

    @Test
    @DisplayName("No deja renombrar una categoria con el nombre de otra")
    void noDuplicaNombreDeCategoria() {
        Categoria ropa = new Categoria(2L, "Ropa", true, false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(alimentos));
        when(categoriaRepository.findByNombre("Ropa")).thenReturn(Optional.of(ropa));

        assertThrows(BusinessException.class,
                () -> service.actualizar(1L, new CategoriaRequest("Ropa", false, true)));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Falla al actualizar una categoria que no existe")
    void categoriaInexistente() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.actualizar(99L, new CategoriaRequest("X", false, false)));
    }

    @Test
    @DisplayName("Exige nombre y los dos indicadores al actualizar una categoria")
    void validaDatosDeCategoria() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(alimentos));

        assertThrows(BusinessException.class,
                () -> service.actualizar(1L, new CategoriaRequest(" ", false, false)));
        assertThrows(BusinessException.class,
                () -> service.actualizar(1L, new CategoriaRequest("Alimentos", null, false)));
    }

    @Test
    @DisplayName("Renombra una subcategoria sin cambiarle la categoria")
    void actualizaSubcategoria() {
        when(subcategoriaRepository.findById(10L)).thenReturn(Optional.of(arroz));
        when(subcategoriaRepository.findByNombre("Arroz largo fino")).thenReturn(Optional.empty());

        SubcategoriaResponse respuesta = service.actualizarSubcategoria(10L, new SubcategoriaRequest("Arroz largo fino"));

        assertEquals("Arroz largo fino", respuesta.nombre());
        assertEquals(1L, respuesta.categoriaId());
    }

    @Test
    @DisplayName("No deja renombrar una subcategoria con el nombre de otra")
    void noDuplicaNombreDeSubcategoria() {
        Subcategoria fideos = new Subcategoria(11L, "Fideos", alimentos);
        when(subcategoriaRepository.findById(10L)).thenReturn(Optional.of(arroz));
        when(subcategoriaRepository.findByNombre("Fideos")).thenReturn(Optional.of(fideos));

        assertThrows(BusinessException.class,
                () -> service.actualizarSubcategoria(10L, new SubcategoriaRequest("Fideos")));
    }

    @Test
    @DisplayName("Falla al actualizar una subcategoria que no existe")
    void subcategoriaInexistente() {
        when(subcategoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.actualizarSubcategoria(99L, new SubcategoriaRequest("X")));
    }

    @Test
    @DisplayName("Elimina una subcategoria que nada usa")
    void eliminaSubcategoriaSinUso() {
        when(subcategoriaRepository.findById(10L)).thenReturn(Optional.of(arroz));

        service.eliminarSubcategoria(10L);

        verify(subcategoriaRepository).deleteById(10L);
    }

    @Test
    @DisplayName("No elimina una subcategoria en uso y dice quien la usa")
    void noEliminaSubcategoriaEnUso() {
        when(subcategoriaRepository.findById(10L)).thenReturn(Optional.of(arroz));
        when(subcategoriaRepository.contarDonaciones(10L)).thenReturn(3L);
        when(subcategoriaRepository.contarNecesidades(10L)).thenReturn(1L);

        BusinessException error = assertThrows(BusinessException.class, () -> service.eliminarSubcategoria(10L));

        assertEquals("No se puede eliminar la subcategoría 'Arroz': la usan 3 donaciones y 1 necesidad",
                error.getMessage());
        verify(subcategoriaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Eliminar una categoria borra todas sus subcategorias")
    void eliminaCategoriaConSusSubcategorias() {
        Subcategoria fideos = new Subcategoria(11L, "Fideos", alimentos);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(alimentos));
        when(subcategoriaRepository.findByCategoriaId(1L)).thenReturn(List.of(arroz, fideos));

        service.eliminarCategoria(1L);

        verify(subcategoriaRepository).deleteById(10L);
        verify(subcategoriaRepository).deleteById(11L);
        verify(categoriaRepository).deleteById(1L);
    }

    @Test
    @DisplayName("No elimina una categoria si alguna de sus subcategorias esta en uso")
    void noEliminaCategoriaConSubcategoriaEnUso() {
        Subcategoria fideos = new Subcategoria(11L, "Fideos", alimentos);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(alimentos));
        when(subcategoriaRepository.findByCategoriaId(1L)).thenReturn(List.of(arroz, fideos));
        when(subcategoriaRepository.contarNecesidades(11L)).thenReturn(2L);

        BusinessException error = assertThrows(BusinessException.class, () -> service.eliminarCategoria(1L));

        assertEquals("No se puede eliminar la categoría 'Alimentos': la usan 2 necesidades", error.getMessage());
        verify(subcategoriaRepository, never()).deleteById(any());
        verify(categoriaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Los usos de una categoria suman los de sus subcategorias")
    void usosDeCategoria() {
        Subcategoria fideos = new Subcategoria(11L, "Fideos", alimentos);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(alimentos));
        when(subcategoriaRepository.findByCategoriaId(1L)).thenReturn(List.of(arroz, fideos));
        when(subcategoriaRepository.contarDonaciones(10L)).thenReturn(2L);
        when(subcategoriaRepository.contarDonaciones(11L)).thenReturn(1L);
        when(subcategoriaRepository.contarNecesidades(11L)).thenReturn(4L);

        UsosCatalogoResponse usos = service.usosCategoria(1L);

        assertEquals(3L, usos.donaciones());
        assertEquals(4L, usos.necesidades());
    }
}
