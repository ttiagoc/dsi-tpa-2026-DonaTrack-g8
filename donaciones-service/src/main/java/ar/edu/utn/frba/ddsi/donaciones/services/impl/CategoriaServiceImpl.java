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
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.UsosCatalogoResponse;
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
        validarCategoria(request, null);
        Categoria categoria = new Categoria(request.nombre().trim(), request.pideEstado(), request.esPerecedero());
        return toCategoriaResponse(categoriaRepository.save(categoria));
    }

    @Override
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarCategoria(id);
        validarCategoria(request, id);
        categoria.setNombre(request.nombre().trim());
        categoria.setPideEstado(request.pideEstado());
        categoria.setEsPerecedero(request.esPerecedero());
        return toCategoriaResponse(categoriaRepository.save(categoria));
    }

    /** idActual: la categoria que se esta editando (null en el alta), para que no choque con su propio nombre. */
    private void validarCategoria(CategoriaRequest request, Long idActual) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre de la categoría no puede estar vacío");
        }
        if (request.pideEstado() == null || request.esPerecedero() == null) {
            throw new BusinessException("La categoría debe indicar si pide estado y si es perecedera");
        }
        String nombre = request.nombre().trim();
        categoriaRepository.findByNombre(nombre)
                .filter(otra -> !otra.getId().equals(idActual))
                .ifPresent(otra -> {
                    throw new BusinessException("Ya existe una categoría con el nombre '" + nombre + "'");
                });
    }

    public List<SubcategoriaResponse> obtenerSubcategorias(Long categoriaId) {
        buscarCategoria(categoriaId);
        return subcategoriaRepository.findByCategoriaId(categoriaId).stream()
                .map(this::toSubcategoriaResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubcategoriaResponse> obtenerTodasLasSubcategorias() {
        return subcategoriaRepository.findAll().stream()
                .map(this::toSubcategoriaResponse)
                .collect(Collectors.toList());
    }

    public SubcategoriaResponse crearSubcategoria(Long categoriaId, SubcategoriaRequest request) {
        Categoria categoria = buscarCategoria(categoriaId);
        validarSubcategoria(request, null);
        Subcategoria subcategoria = new Subcategoria(request.nombre().trim(), categoria);
        return toSubcategoriaResponse(subcategoriaRepository.save(subcategoria));
    }

    @Override
    public SubcategoriaResponse actualizarSubcategoria(Long id, SubcategoriaRequest request) {
        Subcategoria subcategoria = buscarSubcategoria(id);
        validarSubcategoria(request, id);
        subcategoria.setNombre(request.nombre().trim());
        return toSubcategoriaResponse(subcategoriaRepository.save(subcategoria));
    }

    private void validarSubcategoria(SubcategoriaRequest request, Long idActual) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre de la subcategoría no puede estar vacío");
        }
        String nombre = request.nombre().trim();
        subcategoriaRepository.findByNombre(nombre)
                .filter(otra -> !otra.getId().equals(idActual))
                .ifPresent(otra -> {
                    throw new BusinessException("Ya existe una subcategoría con el nombre '" + nombre + "'");
                });
    }

    // ---- Baja: solo de lo que no esta en uso ----

    @Override
    public UsosCatalogoResponse usosSubcategoria(Long id) {
        return usos(buscarSubcategoria(id));
    }

    @Override
    public UsosCatalogoResponse usosCategoria(Long id) {
        buscarCategoria(id);
        return subcategoriaRepository.findByCategoriaId(id).stream()
                .map(this::usos)
                .reduce(new UsosCatalogoResponse(0, 0), (a, b) ->
                        new UsosCatalogoResponse(a.donaciones() + b.donaciones(), a.necesidades() + b.necesidades()));
    }

    @Override
    public void eliminarSubcategoria(Long id) {
        Subcategoria subcategoria = buscarSubcategoria(id);
        UsosCatalogoResponse usos = usos(subcategoria);
        if (enUso(usos)) {
            throw new BusinessException("No se puede eliminar la subcategoría '" + subcategoria.getNombre()
                    + "': " + describir(usos));
        }
        subcategoriaRepository.deleteById(id);
    }

    /** Borra la categoria con todas sus subcategorias, si ninguna esta en uso. */
    @Override
    public void eliminarCategoria(Long id) {
        Categoria categoria = buscarCategoria(id);
        UsosCatalogoResponse usos = usosCategoria(id);
        if (enUso(usos)) {
            throw new BusinessException("No se puede eliminar la categoría '" + categoria.getNombre()
                    + "': " + describir(usos));
        }
        subcategoriaRepository.findByCategoriaId(id).forEach(sub -> subcategoriaRepository.deleteById(sub.getId()));
        categoriaRepository.deleteById(id);
    }

    private UsosCatalogoResponse usos(Subcategoria subcategoria) {
        return new UsosCatalogoResponse(subcategoriaRepository.contarDonaciones(subcategoria.getId()),
                subcategoriaRepository.contarNecesidades(subcategoria.getId()));
    }

    private static boolean enUso(UsosCatalogoResponse usos) {
        return usos.donaciones() > 0 || usos.necesidades() > 0;
    }

    /** "la usan 3 donaciones y 1 necesidad" */
    private static String describir(UsosCatalogoResponse usos) {
        List<String> partes = new java.util.ArrayList<>();
        if (usos.donaciones() > 0) {
            partes.add(usos.donaciones() + (usos.donaciones() == 1 ? " donación" : " donaciones"));
        }
        if (usos.necesidades() > 0) {
            partes.add(usos.necesidades() + (usos.necesidades() == 1 ? " necesidad" : " necesidades"));
        }
        return "la usan " + String.join(" y ", partes);
    }

    private Subcategoria buscarSubcategoria(Long id) {
        return subcategoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una subcategoría con el id: " + id));
    }

    private Categoria buscarCategoria(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una categoría con el id: " + categoriaId));
    }

    private CategoriaResponse toCategoriaResponse(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getPideEstado(), c.getEsPerecedero());
    }

    private SubcategoriaResponse toSubcategoriaResponse(Subcategoria s) {
        return new SubcategoriaResponse(s.getId(), s.getNombre(), s.getCategoria().getId(),
                s.getCategoria().getNombre());
    }
}
