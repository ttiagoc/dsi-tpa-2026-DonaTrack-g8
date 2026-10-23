package ar.edu.utn.frba.ddsi.web.exceptions;

/** El refresh token ya no sirve: se cierra la sesion y se vuelve al login con un aviso. */
public class SesionVencidaException extends RuntimeException {
    public SesionVencidaException() {
        super("La sesion vencio");
    }
}
