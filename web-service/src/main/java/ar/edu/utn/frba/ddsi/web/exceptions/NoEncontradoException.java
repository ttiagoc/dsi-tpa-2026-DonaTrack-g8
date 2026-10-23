package ar.edu.utn.frba.ddsi.web.exceptions;

/** Para que los controllers muestren la pagina 404 (ej. un id que no existe). */
public class NoEncontradoException extends RuntimeException {
    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
