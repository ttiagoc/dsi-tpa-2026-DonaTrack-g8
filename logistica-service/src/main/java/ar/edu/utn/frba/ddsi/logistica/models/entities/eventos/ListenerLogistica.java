package ar.edu.utn.frba.ddsi.logistica.models.entities.eventos;

public interface ListenerLogistica<T extends EventoLogistica> {
    void ejecutar(T evento);
}
