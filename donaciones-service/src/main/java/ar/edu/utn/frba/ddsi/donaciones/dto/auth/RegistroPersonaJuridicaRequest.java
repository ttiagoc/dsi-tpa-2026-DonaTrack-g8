package ar.edu.utn.frba.ddsi.donaciones.dto.auth;

import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaJuridicaRequest;

public record RegistroPersonaJuridicaRequest(String username, String password, PersonaJuridicaRequest donante) {
}
