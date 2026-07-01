package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

import java.util.List;

public record ObtenerDonacionesAsignadasResponse(
    List<DonacionAsignadaInfo> donaciones
) {}
