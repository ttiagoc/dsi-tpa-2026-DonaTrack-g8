package ar.edu.utn.frba.ddsi.common.exceptions;

/** Falta el token, es invalido o vencio, o las credenciales no coinciden: se responde 401. */
public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException(String message) {
        super(message);
    }
}
