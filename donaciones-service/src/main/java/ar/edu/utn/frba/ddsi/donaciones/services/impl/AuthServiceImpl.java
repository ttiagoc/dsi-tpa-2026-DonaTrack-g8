package ar.edu.utn.frba.ddsi.donaciones.services.impl;

import java.util.function.Supplier;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.NoAutenticadoException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.security.JwtUtil;
import ar.edu.utn.frba.ddsi.common.security.Rol;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.LoginRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RefreshRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RegistroEntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RegistroPersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RegistroPersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.SesionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.TokensResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.UsuarioResponse;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.usuarios.Usuario;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.UsuarioRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.AuthService;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int LONGITUD_MINIMA_PASSWORD = 8;
    // mismo mensaje para usuario inexistente y contrasenia incorrecta: no se revela que usuarios existen
    private static final String CREDENCIALES_INVALIDAS = "Usuario o contrasenia incorrectos";

    private final UsuarioRepository usuarioRepository;
    private final DonanteRepository donanteRepository;
    private final DonanteService donanteService;
    private final EntidadBeneficiariaService entidadBeneficiariaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, DonanteRepository donanteRepository,
            DonanteService donanteService, EntidadBeneficiariaService entidadBeneficiariaService,
            PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.donanteRepository = donanteRepository;
        this.donanteService = donanteService;
        this.entidadBeneficiariaService = entidadBeneficiariaService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public SesionResponse login(LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            throw new NoAutenticadoException(CREDENCIALES_INVALIDAS);
        }
        Usuario usuario = usuarioRepository.buscarPorUsername(request.username())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new NoAutenticadoException(CREDENCIALES_INVALIDAS));
        return sesionDe(usuario);
    }

    // Se relee el usuario para que un cambio de rol o una baja se reflejen en el access token nuevo
    @Override
    public TokensResponse refresh(RefreshRequest request) {
        if (request == null || request.refreshToken() == null) {
            throw new NoAutenticadoException("Refresh token invalido");
        }
        String username = jwtUtil.leerRefreshToken(request.refreshToken());
        Usuario usuario = usuarioRepository.buscarPorUsername(username)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new NoAutenticadoException("El usuario ya no esta habilitado"));
        return new TokensResponse(jwtUtil.generarAccessToken(usuario.comoAutenticado()), request.refreshToken());
    }

    @Override
    public SesionResponse registrarPersonaHumana(RegistroPersonaHumanaRequest request) {
        validarCredencialesNuevas(request.username(), request.password());
        if (request.donante() == null) {
            throw new BusinessException("Faltan los datos del donante");
        }
        Long donanteId = donanteService.crearPersonaHumana(request.donante()).id();
        return registrar(request.username(), request.password(), Rol.DONANTE, donanteId,
                () -> donanteService.eliminar(donanteId));
    }

    @Override
    public SesionResponse registrarPersonaJuridica(RegistroPersonaJuridicaRequest request) {
        validarCredencialesNuevas(request.username(), request.password());
        if (request.donante() == null) {
            throw new BusinessException("Faltan los datos del donante");
        }
        Long donanteId = donanteService.crearPersonaJuridica(request.donante()).id();
        return registrar(request.username(), request.password(), Rol.DONANTE, donanteId,
                () -> donanteService.eliminar(donanteId));
    }

    @Override
    public SesionResponse registrarEntidadBeneficiaria(RegistroEntidadBeneficiariaRequest request) {
        validarCredencialesNuevas(request.username(), request.password());
        if (request.entidad() == null) {
            throw new BusinessException("Faltan los datos de la entidad beneficiaria");
        }
        Long entidadId = entidadBeneficiariaService.crear(request.entidad()).id();
        return registrar(request.username(), request.password(), Rol.ENTIDAD_BENEFICIARIA, entidadId,
                () -> entidadBeneficiariaService.eliminar(entidadId));
    }

    @Override
    public UsuarioResponse obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(this::toUsuarioResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro un usuario con el id: " + usuarioId));
    }

    @Override
    public UsuarioResponse crearUsuarioParaDonante(Long donanteId, String username, String password) {
        validarCredencialesNuevas(username, password);
        if (!donanteRepository.existsById(donanteId)) {
            throw new ResourceNotFoundException("No se encontro un donante con el id: " + donanteId);
        }
        return toUsuarioResponse(guardar(username, password, Rol.DONANTE, donanteId));
    }

    @Override
    public void crearAdministradorSiNoExiste(String username, String password) {
        if (usuarioRepository.existeUsername(username)) {
            return;
        }
        guardar(username, password, Rol.ADMINISTRADOR, null);
    }

    // El perfil se crea antes que el usuario (los services existentes validan sus datos). Si despues falla
    // el usuario, se borra el perfil para no dejarlo huerfano: no hay una transaccion que abarque a ambos.
    private SesionResponse registrar(String username, String password, Rol rol, Long perfilId,
            Supplier<Boolean> deshacerPerfil) {
        try {
            return sesionDe(guardar(username, password, rol, perfilId));
        } catch (RuntimeException e) {
            deshacerPerfil.get();
            throw e;
        }
    }

    private Usuario guardar(String username, String password, Rol rol, Long perfilId) {
        return usuarioRepository.save(new Usuario(username, passwordEncoder.encode(password), rol, perfilId));
    }

    private void validarCredencialesNuevas(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("El nombre de usuario es obligatorio");
        }
        if (password == null || password.length() < LONGITUD_MINIMA_PASSWORD) {
            throw new BusinessException(
                    "La contrasenia debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres");
        }
        if (usuarioRepository.existeUsername(username)) {
            throw new BusinessException("El nombre de usuario " + Usuario.normalizar(username) + " ya esta en uso");
        }
    }

    private SesionResponse sesionDe(Usuario usuario) {
        return new SesionResponse(
                jwtUtil.generarAccessToken(usuario.comoAutenticado()),
                jwtUtil.generarRefreshToken(usuario.getUsername()),
                toUsuarioResponse(usuario));
    }

    private UsuarioResponse toUsuarioResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getRol(), usuario.getPerfilId());
    }
}
