package ar.edu.utn.frba.ddsi.logistica.dto.monitoreo;

public record UbicacionRequest(
        Double latitud,
        Double longitud,
        Double velocidad) {
}