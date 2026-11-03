package ar.edu.utn.frba.ddsi.web.dto.donaciones;

import java.util.List;

public record DonacionAltaRequest(
        String descripcion,
        Long idDonante,
        List<BienAltaRequest> bienes
) {
}

