package ar.edu.utn.frba.ddsi.web.dto.catalogo;

import java.util.List;

/** Una categoria con los nombres de sus subcategorias: un <optgroup> de los selects. */
public record Rubro(String categoria, List<String> subcategorias) {
}
