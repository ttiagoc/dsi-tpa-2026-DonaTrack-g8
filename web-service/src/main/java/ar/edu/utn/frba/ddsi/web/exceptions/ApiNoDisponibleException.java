package ar.edu.utn.frba.ddsi.web.exceptions;

/** La API no respondio (caida, timeout o URL mal configurada). */
public class ApiNoDisponibleException extends RuntimeException {
    public ApiNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
