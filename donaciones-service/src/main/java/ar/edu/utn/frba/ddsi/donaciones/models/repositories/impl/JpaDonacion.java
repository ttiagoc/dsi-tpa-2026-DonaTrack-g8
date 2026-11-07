package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.common.dtos.pagina.PaginaResponse;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;

@Repository
public class JpaDonacion extends RepositorioJpa<Donacion> implements DonacionRepository {

    public JpaDonacion() {
        super(Donacion.class);
    }

    @Override
    public List<Donacion> saveAll(List<Donacion> donaciones) {
        return withTransaction(() -> {
            List<Donacion> guardadas = new ArrayList<>();
            for (Donacion donacion : donaciones) {
                guardadas.add(save(donacion));
            }
            return guardadas;
        });
    }

    @Override
    public List<Donacion> buscarPorEstado(TipoEstadoDonacion estadoBuscado) {
        if (estadoBuscado == null) {
            return new ArrayList<>();
        }
        return createQuery("select d from Donacion d where d.estadoActual = :estado", Donacion.class)
                .setParameter("estado", estadoBuscado)
                .getResultList();
    }

    // las mas recientes primero
    @Override
    public List<Donacion> buscarPorEntidad(Long entidadId) {
      return createQuery("select d from Donacion d where d.entidadBeneficiariaAsignada.id = :entidadId"
          + " order by d.fecha desc, d.id desc", Donacion.class)
          .setParameter("entidadId", entidadId)
          .getResultList();
    }

    @Override
    public List<Donacion> buscar(Long donanteId, TipoEstadoDonacion estado, Long categoriaId,
                                 List<Long> subcategoriaIds, Long entidadId) {
      StringBuilder jpq1 = new StringBuilder("select distinct d from Donacion d left join d.subcategoria s left join s.categoria c where 1=1");

      if (donanteId != null) {
        jpq1.append(" and d.donante.id = :donanteId");
      }
      if (estado != null) {
        jpq1.append(" and d.estadoActual = :estado");
      }
      if (categoriaId != null) {
        jpq1.append(" and c.id = :categoriaId");
      }
      if (subcategoriaIds != null && !subcategoriaIds.isEmpty()) {
        jpq1.append(" and s.id in :subcategoriaIds");
      }
      if (entidadId != null) {
        jpq1.append(" and d.entidadBeneficiariaAsignada.id = :entidadId");
      }

      jpq1.append(" Order by d.fecha desc");

      var query = createQuery(jpq1.toString(), Donacion.class);

      if (donanteId != null) {
        query.setParameter("donanteId", donanteId);
      }
      if (estado != null) {
        query.setParameter("estado", estado);
      }
      if (categoriaId != null) {
        query.setParameter("categoriaId", categoriaId);
      }
      if (subcategoriaIds != null && !subcategoriaIds.isEmpty()) {
        query.setParameter("subcategoriaIds", subcategoriaIds);
      }
      if (entidadId != null) {
        query.setParameter("entidadId", entidadId);
      }
      return query.getResultList();
    }

    @Override
    public PaginaResponse<Donacion> buscarPaginado(Long donanteId, TipoEstadoDonacion estado, Long categoriaId,
                                                    List<Long> subcategoriaIds, Long entidadId, int page, int size) {
      int paginaSegura = Math.max(1, page);
      int tamanoSeguro = Math.max(1, size);

      StringBuilder where = new StringBuilder(" from Donacion d left join d.subcategoria s left join s.categoria c where 1=1");

      if (donanteId != null) {
        where.append(" and d.donante.id = :donanteId");
      }
      if (estado != null) {
        where.append(" and d.estadoActual = :estado");
      }
      if (categoriaId != null) {
        where.append(" and c.id = :categoriaId");
      }
      if (subcategoriaIds != null && !subcategoriaIds.isEmpty()) {
        where.append(" and s.id in :subcategoriaIds");
      }
      if (entidadId != null) {
        where.append(" and d.entidadBeneficiariaAsignada.id = :entidadId");
      }

      var countQuery = createQuery("select count(distinct d)" + where, Long.class);
      if (donanteId != null) countQuery.setParameter("donanteId", donanteId);
      if (estado != null) countQuery.setParameter("estado", estado);
      if (categoriaId != null) countQuery.setParameter("categoriaId", categoriaId);
      if (subcategoriaIds != null && !subcategoriaIds.isEmpty()) countQuery.setParameter("subcategoriaIds", subcategoriaIds);
      if (entidadId != null) countQuery.setParameter("entidadId", entidadId);

      Long total = countQuery.getSingleResult();
      int totalPaginas = Math.max(1, (int) Math.ceil((double) total / tamanoSeguro));
      int offset = (paginaSegura - 1) * tamanoSeguro;

      var dataQuery = createQuery("select distinct d" + where + " order by d.fecha desc, d.id desc", Donacion.class);
      if (donanteId != null) dataQuery.setParameter("donanteId", donanteId);
      if (estado != null) dataQuery.setParameter("estado", estado);
      if (categoriaId != null) dataQuery.setParameter("categoriaId", categoriaId);
      if (subcategoriaIds != null && !subcategoriaIds.isEmpty()) dataQuery.setParameter("subcategoriaIds", subcategoriaIds);
      if (entidadId != null) dataQuery.setParameter("entidadId", entidadId);

      dataQuery.setFirstResult(offset);
      dataQuery.setMaxResults(tamanoSeguro);

      List<Donacion> elementos = dataQuery.getResultList();
      return new PaginaResponse<>(elementos, paginaSegura, totalPaginas, total, tamanoSeguro);
    }
}
