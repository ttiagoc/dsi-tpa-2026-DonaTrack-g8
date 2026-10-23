package ar.edu.utn.frba.ddsi.web.exceptions;

/** La API respondio con un error (4xx/5xx). El mensaje es el que devolvio el backend, listo para mostrar. */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public boolean esErrorDelUsuario() {
        return status >= 400 && status < 500;
    }
}
