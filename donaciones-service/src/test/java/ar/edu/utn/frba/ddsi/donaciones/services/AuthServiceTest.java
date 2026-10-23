package ar.edu.utn.frba.ddsi.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.NoAutenticadoException;
import ar.edu.utn.frba.ddsi.common.security.JwtUtil;
import ar.edu.utn.frba.ddsi.common.security.Rol;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.LoginRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RefreshRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RegistroPersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.SesionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.DonanteResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.usuarios.Usuario;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.UsuarioRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.impl.AuthServiceImpl;

@DisplayName("Tests del AuthService")
class AuthServiceTest {

    private UsuarioRepository usuarioRepository;
    private DonanteRepository donanteRepository;
    private DonanteService donanteService;
    private final JwtUtil jwtUtil = new JwtUtil("secreto-de-test-con-al-menos-32-bytes-0123");
    private AuthServiceImpl authService;
    private final List<Usuario> guardados = new ArrayList<>();

    private final PersonaHumanaRequest ana = new PersonaHumanaRequest("Ana", "Perez", null, "12345678", "F",
            "Medrano 951", List.of(), null);

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        donanteRepository = mock(DonanteRepository.class);
        donanteService = mock(DonanteService.class);
        authService = new AuthServiceImpl(usuarioRepository, donanteRepository, donanteService,
                mock(EntidadBeneficiariaService.class), new BCryptPasswordEncoder(4), jwtUtil);

        when(usuarioRepository.save(any())).thenAnswer(invocacion -> {
            Usuario usuario = invocacion.getArgument(0);
            usuario.setId((long) guardados.size() + 1);
            guardados.add(usuario);
            return usuario;
        });
        when(usuarioRepository.buscarPorUsername(any())).thenAnswer(invocacion -> guardados.stream()
                .filter(u -> u.getUsername().equals(Usuario.normalizar(invocacion.getArgument(0))))
                .findFirst());
        when(usuarioRepository.existeUsername(any())).thenAnswer(invocacion -> guardados.stream()
                .anyMatch(u -> u.getUsername().equals(Usuario.normalizar(invocacion.getArgument(0)))));
        when(donanteService.crearPersonaHumana(any())).thenReturn(new DonanteResponse(42L, "Persona Humana", "x"));
    }

    @Test
    @DisplayName("Registrar un donante crea su perfil y un usuario con la contrasenia hasheada")
    void registroCreaPerfilYUsuario() {
        SesionResponse sesion = authService.registrarPersonaHumana(
                new RegistroPersonaHumanaRequest("Ana", "clave-segura", ana));

        Usuario usuario = guardados.get(0);
        assertEquals("ana", usuario.getUsername());
        assertEquals(Rol.DONANTE, usuario.getRol());
        assertEquals(42L, usuario.getPerfilId());
        assertNotEquals("clave-segura", usuario.getPasswordHash());
        assertEquals(42L, jwtUtil.leerAccessToken(sesion.accessToken()).perfilId());
        assertEquals(Rol.DONANTE, sesion.usuario().rol());
    }

    @Test
    @DisplayName("Con un username tomado no se crea el perfil")
    void usernameTomadoNoCreaPerfil() {
        authService.crearAdministradorSiNoExiste("ana", "clave-segura");

        assertThrows(BusinessException.class, () -> authService.registrarPersonaHumana(
                new RegistroPersonaHumanaRequest("ANA", "otra-clave-segura", ana)));
        verify(donanteService, never()).crearPersonaHumana(any());
    }

    @Test
    @DisplayName("Una contrasenia corta se rechaza antes de crear el perfil")
    void contraseniaCorta() {
        assertThrows(BusinessException.class, () -> authService.registrarPersonaHumana(
                new RegistroPersonaHumanaRequest("ana", "corta", ana)));
        verify(donanteService, never()).crearPersonaHumana(any());
    }

    @Test
    @DisplayName("Si falla el guardado del usuario, se borra el perfil recien creado")
    void fallaUsuarioBorraPerfil() {
        doThrow(new RuntimeException("falla la base")).when(usuarioRepository).save(any());

        assertThrows(RuntimeException.class, () -> authService.registrarPersonaHumana(
                new RegistroPersonaHumanaRequest("ana", "clave-segura", ana)));
        verify(donanteService).eliminar(42L);
    }

    @Test
    @DisplayName("Login: acepta la contrasenia correcta y rechaza la incorrecta o un usuario inexistente")
    void login() {
        authService.registrarPersonaHumana(new RegistroPersonaHumanaRequest("ana", "clave-segura", ana));

        SesionResponse sesion = authService.login(new LoginRequest("Ana", "clave-segura"));

        assertEquals("ana", jwtUtil.leerAccessToken(sesion.accessToken()).username());
        assertThrows(NoAutenticadoException.class, () -> authService.login(new LoginRequest("ana", "incorrecta")));
        assertThrows(NoAutenticadoException.class, () -> authService.login(new LoginRequest("nadie", "clave-segura")));
    }

    @Test
    @DisplayName("Un usuario dado de baja no puede entrar ni renovar el token")
    void usuarioInactivo() {
        SesionResponse sesion = authService.registrarPersonaHumana(
                new RegistroPersonaHumanaRequest("ana", "clave-segura", ana));
        guardados.get(0).setActivo(false);

        assertThrows(NoAutenticadoException.class, () -> authService.login(new LoginRequest("ana", "clave-segura")));
        assertThrows(NoAutenticadoException.class,
                () -> authService.refresh(new RefreshRequest(sesion.refreshToken())));
    }

    @Test
    @DisplayName("Refresh devuelve un access token valido y rechaza un access token")
    void refresh() {
        SesionResponse sesion = authService.registrarPersonaHumana(
                new RegistroPersonaHumanaRequest("ana", "clave-segura", ana));

        String nuevo = authService.refresh(new RefreshRequest(sesion.refreshToken())).accessToken();

        assertEquals(Rol.DONANTE, jwtUtil.leerAccessToken(nuevo).rol());
        assertThrows(NoAutenticadoException.class,
                () -> authService.refresh(new RefreshRequest(sesion.accessToken())));
    }

    @Test
    @DisplayName("El administrador inicial se crea una sola vez")
    void administradorInicial() {
        authService.crearAdministradorSiNoExiste("admin", "admin1234");
        authService.crearAdministradorSiNoExiste("admin", "admin1234");

        assertEquals(1, guardados.size());
        assertEquals(Rol.ADMINISTRADOR, guardados.get(0).getRol());
        assertTrue(guardados.get(0).getPerfilId() == null);
    }

    @Test
    @DisplayName("Crear el usuario de un donante importado exige que el donante exista")
    void usuarioParaDonanteExistente() {
        when(donanteRepository.existsById(7L)).thenReturn(true);

        assertEquals(7L, authService.crearUsuarioParaDonante(7L, "importado", "temporal-123").perfilId());
        assertThrows(RuntimeException.class, () -> authService.crearUsuarioParaDonante(8L, "otro", "temporal-123"));
        assertEquals(Optional.of(7L), guardados.stream().map(Usuario::getPerfilId).findFirst());
    }
}
