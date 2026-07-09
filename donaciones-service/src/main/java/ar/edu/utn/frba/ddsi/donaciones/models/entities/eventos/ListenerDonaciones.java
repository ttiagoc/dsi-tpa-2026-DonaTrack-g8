package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

public interface ListenerDonaciones<T extends EventoDonaciones> {
    void ejecutar(T evento);
}
