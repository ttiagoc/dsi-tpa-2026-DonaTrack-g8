package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.InicioRutaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;

public interface EventoService {

    void verificarInactividadDonantes();

    void notificarAusenciaDonante(Donante donante);

    void iniciarRuta(InicioRutaRequest request);

    void confirmarEntregaExitosa(ConfirmacionEntregaExitosaRequest request);

    void notificarEntregaFallida(Long donacionId, String motivo);
}
