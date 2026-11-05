package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.UsosCatalogoResponse;

public interface CategoriaService {

    List<CategoriaResponse> obtenerTodas();

    CategoriaResponse crear(CategoriaRequest request);

    CategoriaResponse actualizar(Long id, CategoriaRequest request);

    List<SubcategoriaResponse> obtenerSubcategorias(Long categoriaId);

    List<SubcategoriaResponse> obtenerTodasLasSubcategorias();

    SubcategoriaResponse crearSubcategoria(Long categoriaId, SubcategoriaRequest request);

    SubcategoriaResponse actualizarSubcategoria(Long id, SubcategoriaRequest request);

    UsosCatalogoResponse usosCategoria(Long id);

    UsosCatalogoResponse usosSubcategoria(Long id);

    /** Borra la categoria y todas sus subcategorias. Falla si alguna esta en uso. */
    void eliminarCategoria(Long id);

    /** Falla si alguna donacion o necesidad la usa. */
    void eliminarSubcategoria(Long id);
}
