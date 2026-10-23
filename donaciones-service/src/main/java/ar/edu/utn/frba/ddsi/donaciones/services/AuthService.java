package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.auth.LoginRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RefreshRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RegistroEntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RegistroPersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RegistroPersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.SesionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.TokensResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.UsuarioResponse;

public interface AuthService {

    SesionResponse login(LoginRequest request);

    TokensResponse refresh(RefreshRequest request);

    SesionResponse registrarPersonaHumana(RegistroPersonaHumanaRequest request);

    SesionResponse registrarPersonaJuridica(RegistroPersonaJuridicaRequest request);

    SesionResponse registrarEntidadBeneficiaria(RegistroEntidadBeneficiariaRequest request);

    UsuarioResponse obtenerUsuario(Long usuarioId);

    /** Crea el usuario de un donante que ya existe (lo usa la importacion CSV). */
    UsuarioResponse crearUsuarioParaDonante(Long donanteId, String username, String password);

    /** Crea el administrador inicial si todavia no existe un usuario con ese nombre. */
    void crearAdministradorSiNoExiste(String username, String password);
}
