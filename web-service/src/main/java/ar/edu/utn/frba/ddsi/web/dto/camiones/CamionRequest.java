package ar.edu.utn.frba.ddsi.web.dto.camiones;

public record CamionRequest(
    String patente,
    Double capacidadVolumen,
    Double altura,
    Double capacidadCarga,
    ChoferDto chofer
) {}

