package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

import java.util.List;

public record DonacionRequest(String descripcion, Long idDonante, List<BienRequest> bienes) {
}