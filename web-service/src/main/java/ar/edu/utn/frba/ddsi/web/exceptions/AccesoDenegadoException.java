package ar.edu.utn.frba.ddsi.web.exceptions;

/** Rol equivocado para la pagina, o la API devolvio 403. Termina en la pagina 403. */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
