package ar.edu.utn.frba.ddsi.web.exceptions;

/** Pagina protegida sin sesion: se redirige al login. */
public class SesionRequeridaException extends RuntimeException {
    public SesionRequeridaException() {
        super("Se requiere iniciar sesion");
    }
}
