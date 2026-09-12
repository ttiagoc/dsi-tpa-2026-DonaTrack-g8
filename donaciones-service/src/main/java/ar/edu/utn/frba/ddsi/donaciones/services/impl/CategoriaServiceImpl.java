package ar.edu.utn.frba.ddsi.donaciones.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaResponse;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.CategoriaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.SubcategoriaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.CategoriaService;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository,
            SubcategoriaRepository subcategoriaRepository) {
        this.categoriaRepository = categoriaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
    }

    public List<CategoriaResponse> obtenerTodas() {
        return categoriaRepository.findAll().stream()
                .map(this::toCategoriaResponse)
                .collect(Collectors.toList());
    }

    public CategoriaResponse crear(CategoriaRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre de la categoria no puede ser nulo ni estar vacio");
        }
        if (request.pideEstado() == null || request.esPerecedero() == null) {
            throw new BusinessException("La categoria debe indicar si pide estado y si es perecedera");
        }
        if (categoriaRepository.findByNombre(request.nombre()).isPresent()) {
            throw new BusinessException("Ya existe una categoria con el nombre '" + request.nombre() + "'");
        }

        Categoria categoria = new Categoria(request.nombre(), request.pideEstado(), request.esPerecedero());
        return toCategoriaResponse(categoriaRepository.save(categoria));
    }

    public List<SubcategoriaResponse> obtenerSubcategorias(Long categoriaId) {
        buscarCategoria(categoriaId);
        return subcategoriaRepository.findByCategoriaId(categoriaId).stream()
                .map(this::toSubcategoriaResponse)
                .collect(Collectors.toList());
    }

    public SubcategoriaResponse crearSubcategoria(Long categoriaId, SubcategoriaRequest request) {
        Categoria categoria = buscarCategoria(categoriaId);
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre de la subcategoria no puede ser nulo ni estar vacio");
        }
        if (subcategoriaRepository.findByNombre(request.nombre()).isPresent()) {
            throw new BusinessException("Ya existe una subcategoria con el nombre '" + request.nombre() + "'");
        }

        Subcategoria subcategoria = new Subcategoria(request.nombre(), categoria);
        return toSubcategoriaResponse(subcategoriaRepository.save(subcategoria));
    }

    private Categoria buscarCategoria(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro una categoria con el id: " + categoriaId));
    }

    private CategoriaResponse toCategoriaResponse(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getPideEstado(), c.getEsPerecedero());
    }

    private SubcategoriaResponse toSubcategoriaResponse(Subcategoria s) {
        return new SubcategoriaResponse(s.getId(), s.getNombre(), s.getCategoria().getId(),
                s.getCategoria().getNombre());
    }
}
