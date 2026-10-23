package ar.edu.utn.frba.ddsi.donaciones.dto.auth;

/** Respuesta del login y del registro: los tokens y los datos del usuario, para no tener que decodificar el JWT. */
public record SesionResponse(String accessToken, String refreshToken, UsuarioResponse usuario) {
}
