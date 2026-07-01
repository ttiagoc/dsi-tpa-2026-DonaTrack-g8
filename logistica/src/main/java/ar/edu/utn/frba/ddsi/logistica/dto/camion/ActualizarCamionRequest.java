package ar.edu.utn.frba.ddsi.logistica.dto.camion;

public record ActualizarCamionRequest(
    String patente,
    Double capacidadVolumen,
    Double altura,
    Double capacidadCarga,
    ChoferInfo chofer
) {}
