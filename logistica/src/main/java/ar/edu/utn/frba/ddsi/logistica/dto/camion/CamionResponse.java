package ar.edu.utn.frba.ddsi.logistica.dto.camion;

public record CamionResponse(
        Long id,
        String patente,
        Double capacidadVolumen,
        Double altura,
        Double capacidadCarga,
        ChoferResponse chofer) {
}