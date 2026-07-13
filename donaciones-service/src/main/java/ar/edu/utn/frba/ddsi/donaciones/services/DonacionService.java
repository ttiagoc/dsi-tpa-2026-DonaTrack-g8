package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionAsignadaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.SubirFotosRecepcionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;

public interface DonacionService {

    List<DonacionResponse> obtenerTodas();

    DonacionResponse obtenerPorId(Long id);

    List<DonacionResponse> crear(DonacionRequest request);

    boolean eliminar(Long id);

    EstadoDonacionResponse cambiarEstado(Long id, EstadoDonacionRequest request);

    List<DonacionAsignadaResponse> obtenerDonacionesSegunEstado(String estado, int limit);

    void subirFotosRecepcion(Long id, SubirFotosRecepcionRequest request);

    void confirmarEntregaExitosa(ConfirmacionEntregaExitosaRequest request);
}
