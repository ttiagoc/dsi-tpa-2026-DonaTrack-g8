package ar.edu.utn.frba.ddsi.logistica.dto.donacion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DonacionResponse(
        Long id,
        Long donanteId) {
}
