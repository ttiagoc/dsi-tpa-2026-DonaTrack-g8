package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaResponse;

public interface CategoriaService {

    List<CategoriaResponse> obtenerTodas();

    CategoriaResponse crear(CategoriaRequest request);

    List<SubcategoriaResponse> obtenerSubcategorias(Long categoriaId);

    SubcategoriaResponse crearSubcategoria(Long categoriaId, SubcategoriaRequest request);
}
