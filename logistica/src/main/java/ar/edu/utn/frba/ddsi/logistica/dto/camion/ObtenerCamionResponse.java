package ar.edu.utn.frba.ddsi.logistica.dto.camion;

public record ObtenerCamionResponse(
    Long id,
    String patente,
    Double capacidadVolumen,
    Double altura,
    Double capacidadCarga,
    ChoferInfo chofer
) {}
