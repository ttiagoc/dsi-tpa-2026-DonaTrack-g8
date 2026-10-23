package ar.edu.utn.frba.ddsi.donaciones.dto.auth;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaRequest;

public record RegistroEntidadBeneficiariaRequest(String username, String password, EntidadBeneficiariaRequest entidad) {
}
