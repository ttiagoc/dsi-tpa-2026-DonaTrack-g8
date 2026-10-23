package ar.edu.utn.frba.ddsi.common.exceptions;

/** El usuario esta autenticado pero su rol no puede usar el recurso, o el recurso no es suyo: se responde 403. */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String message) {
        super(message);
    }
}
