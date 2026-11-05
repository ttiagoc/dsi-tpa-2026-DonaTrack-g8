package ar.edu.utn.frba.ddsi.web.dto.catalogo;

import java.util.List;

/** Una fila del ABM de categorias (/admin/categorias): la categoria y sus subcategorias. */
public record CategoriaConSubcategorias(CategoriaResponse categoria, List<SubcategoriaResponse> subcategorias) {
}
