package ar.edu.utn.frba.ddsi.web.dto.catalogo;

import java.util.List;

/**
 * Una categoria con los nombres de sus subcategorias: un <optgroup> de los selects.
 * pideEstado y esPerecedero dicen que campos del bien aplican (estado y fecha de vencimiento).
 */
public record Rubro(String categoria, Boolean pideEstado, Boolean esPerecedero, List<String> subcategorias) {

    public boolean registraEstado() {
        return Boolean.TRUE.equals(pideEstado);
    }

    public boolean vence() {
        return Boolean.TRUE.equals(esPerecedero);
    }
}
