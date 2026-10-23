package ar.edu.utn.frba.ddsi.web.dto.auth;

/** Respuesta de /api/auth/login y de los registros. */
public record SesionResponse(String accessToken, String refreshToken, UsuarioSesion usuario) {
}
