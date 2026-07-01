package ar.edu.utn.frba.ddsi.logistica.dto.camion;

public record CrearCamionRequest(
    String patente,
    Double capacidadVolumen,
    Double altura,
    Double capacidadCarga,
    ChoferInfo chofer
) {}
