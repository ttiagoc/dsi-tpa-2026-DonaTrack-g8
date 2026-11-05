package ar.edu.utn.frba.ddsi.web.dto.catalogo;

/** Alta y edicion de una categoria (POST /api/categorias, PUT /api/categorias/{id}). */
public record CategoriaRequest(String nombre, Boolean pideEstado, Boolean esPerecedero) {
}
