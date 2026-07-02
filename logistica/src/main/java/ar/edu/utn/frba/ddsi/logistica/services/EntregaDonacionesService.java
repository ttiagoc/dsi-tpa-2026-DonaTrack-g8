package ar.edu.utn.frba.ddsi.logistica.services;

public interface EntregaDonacionesService {

    void iniciarRuta(Long rutaId);

    void confirmarEntregaExitosa(Long paradaId, Long rutaId);
}
