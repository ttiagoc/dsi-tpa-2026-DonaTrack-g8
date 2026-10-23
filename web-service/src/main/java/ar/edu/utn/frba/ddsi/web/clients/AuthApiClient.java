package ar.edu.utn.frba.ddsi.web.clients;

import java.net.http.HttpClient;

import ar.edu.utn.frba.ddsi.web.dto.auth.LoginRequest;
import ar.edu.utn.frba.ddsi.web.dto.auth.RefreshRequest;
import ar.edu.utn.frba.ddsi.web.dto.auth.SesionResponse;
import ar.edu.utn.frba.ddsi.web.dto.auth.TokensResponse;
import ar.edu.utn.frba.ddsi.web.dto.auth.UsuarioSesion;
import ar.edu.utn.frba.ddsi.web.dto.registro.RegistroEntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.RegistroPersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.RegistroPersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.exceptions.SesionVencidaException;
import ar.edu.utn.frba.ddsi.web.sesion.SesionUsuario;
import io.javalin.http.Context;

import com.fasterxml.jackson.databind.ObjectMapper;

/** /api/auth de donaciones-service (contrato en el Plan A, seccion 2.1). */
public class AuthApiClient extends ApiClient {

    public AuthApiClient(String donacionesUrl, HttpClient http, ObjectMapper mapper) {
        super(donacionesUrl, http, mapper, null);
    }

    @Override
    protected AuthApiClient auth() {
        return this;
    }

    public SesionUsuario login(String username, String password) {
        return SesionUsuario.de(postPublico("/auth/login", new LoginRequest(username, password), SesionResponse.class));
    }

    public SesionUsuario registrarPersonaHumana(RegistroPersonaHumanaRequest request) {
        return SesionUsuario.de(postPublico("/auth/registro/persona-humana", request, SesionResponse.class));
    }

    public SesionUsuario registrarPersonaJuridica(RegistroPersonaJuridicaRequest request) {
        return SesionUsuario.de(postPublico("/auth/registro/persona-juridica", request, SesionResponse.class));
    }

    public SesionUsuario registrarEntidadBeneficiaria(RegistroEntidadBeneficiariaRequest request) {
        return SesionUsuario.de(postPublico("/auth/registro/entidad-beneficiaria", request, SesionResponse.class));
    }

    public UsuarioSesion me(Context ctx) {
        return get(ctx, "/auth/me", UsuarioSesion.class);
    }

    /** Si el refresh token tambien vencio, se cierra la sesion. */
    void renovarTokens(Context ctx, SesionUsuario sesion) {
        try {
            sesion.actualizarTokens(postPublico("/auth/refresh",
                    new RefreshRequest(sesion.getRefreshToken()), TokensResponse.class));
        } catch (ApiException e) {
            SesionUsuario.cerrar(ctx);
            throw new SesionVencidaException();
        }
    }
}
