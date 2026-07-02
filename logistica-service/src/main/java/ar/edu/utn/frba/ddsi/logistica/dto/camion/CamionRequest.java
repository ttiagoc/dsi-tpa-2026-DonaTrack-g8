package ar.edu.utn.frba.ddsi.logistica.dto.camion;

public record CamionRequest(
    String patente,
    Double capacidadVolumen,
    Double altura,
    Double capacidadCarga,
    ChoferRequest chofer
) {}
