package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.common.dtos.pagina.PaginaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionAsignadaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EntidadConEntregasResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.SubirFotosRecepcionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;

public interface DonacionService {

    List<DonacionResponse> obtenerTodas();

    // las asignadas a la entidad (req. 11), las mas recientes primero
    List<DonacionResponse> obtenerPorEntidad(Long entidadId);

    DonacionResponse obtenerPorId(Long id);

    List<DonacionResponse> crear(DonacionRequest request);

    boolean eliminar(Long id);

    EstadoDonacionResponse cambiarEstado(Long id, EstadoDonacionRequest request);

    List<DonacionAsignadaResponse> obtenerDonacionesSegunEstado(String estado, int limit, int offset);

    // las donaciones entregadas agrupadas por entidad, para el mapa publico (req. 2)
    List<EntidadConEntregasResponse> obtenerEntregasPorEntidad();
    List<EntidadConEntregasResponse> obtenerEntregasPorEntidad(Integer mesesAtras);

    void subirFotosRecepcion(Long id, SubirFotosRecepcionRequest request);

    // la entidad confirma que recibio una donacion entregada, con sus fotos (req. 12)
    DonacionResponse confirmarRecepcion(Long id, SubirFotosRecepcionRequest request);

    void confirmarEntregaExitosa(ConfirmacionEntregaExitosaRequest request);

    List<DonacionResponse> buscar(Long donanteId, String estado, Long categoriaId, List<Long> subcategoriasIds, Long entidadId);
    PaginaResponse<DonacionResponse> buscarPaginado(Long donanteId, String estado, Long categoriaId, List<Long> subcategoriasIds, Long entidadId, int page, int size);
}
