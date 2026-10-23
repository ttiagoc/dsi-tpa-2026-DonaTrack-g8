package ar.edu.utn.frba.ddsi.donaciones.dto.auth;

import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaHumanaRequest;

public record RegistroPersonaHumanaRequest(String username, String password, PersonaHumanaRequest donante) {
}
