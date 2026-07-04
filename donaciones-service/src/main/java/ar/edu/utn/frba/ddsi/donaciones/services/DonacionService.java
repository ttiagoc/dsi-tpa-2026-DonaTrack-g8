package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionAsignadaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionResponse;

public interface DonacionService {

    List<DonacionResponse> obtenerTodas();

    DonacionResponse obtenerPorId(Long id);

    List<DonacionResponse> crear(DonacionRequest request);

    boolean eliminar(Long id);

    EstadoDonacionResponse cambiarEstado(Long id, EstadoDonacionRequest request);

    List<DonacionAsignadaResponse> obtenerDonacionesAsignadas(int limit);

    void donacionesEntregaLista(List<Long> donacionesIds);

    void replanificar(Long id);
}
