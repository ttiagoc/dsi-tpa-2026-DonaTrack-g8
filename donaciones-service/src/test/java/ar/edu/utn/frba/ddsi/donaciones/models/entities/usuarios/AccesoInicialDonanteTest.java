package ar.edu.utn.frba.ddsi.donaciones.models.entities.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.security.Rol;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.UsuarioRepository;

@DisplayName("Tests de AccesoInicialDonante")
class AccesoInicialDonanteTest {

    private UsuarioRepository usuarioRepository;
    private RestTemplate restTemplate;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AccesoInicialDonante acceso;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        restTemplate = mock(RestTemplate.class);
        RestDonacionesConfig config = new RestDonacionesConfig();
        config.setNotificacionesUrl("http://localhost:8082/api");
        config.setWebUrl("http://localhost:7070/");
        when(usuarioRepository.buscarPorRolYPerfilId(any(), any())).thenReturn(Optional.empty());
        when(usuarioRepository.existeUsername(anyString())).thenReturn(false);
        acceso = new AccesoInicialDonante(usuarioRepository, passwordEncoder, restTemplate, config);
    }

    @Test
    @DisplayName("Al donante que carga el administrador se le crea el usuario y se le mandan las credenciales")
    void creaUsuarioYMandaCredenciales() {
        acceso.otorgar(7L, "Ana Perez", "Ana@Mail.com", true);

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuario.capture());
        assertEquals(Rol.DONANTE, usuario.getValue().getRol());
        assertEquals(7L, usuario.getValue().getPerfilId());

        ArgumentCaptor<NotificacionRequest> aviso = ArgumentCaptor.forClass(NotificacionRequest.class);
        verify(restTemplate, timeout(2000)).postForObject(eq("http://localhost:8082/api/notificaciones"),
                aviso.capture(), eq(Void.class));
        String mensaje = aviso.getValue().getMensaje();
        assertEquals("Ana@Mail.com", aviso.getValue().getValor());
        assertEquals("ALTA_USUARIO", aviso.getValue().getTipoEvento());
        assertTrue(mensaje.contains("http://localhost:7070/login"), mensaje);
        assertTrue(mensaje.contains("usuario " + usuario.getValue().getUsername()), mensaje);

        // la contrasenia del mensaje es la que quedo guardada
        String password = mensaje.substring(mensaje.indexOf("temporal ") + "temporal ".length(), mensaje.length() - 1);
        assertTrue(passwordEncoder.matches(password, usuario.getValue().getPasswordHash()));
    }

    @Test
    @DisplayName("Un donante que ya tiene usuario y se actualiza no recibe ningún aviso")
    void donanteExistenteConUsuarioNoSeAvisa() {
        when(usuarioRepository.buscarPorRolYPerfilId(Rol.DONANTE, 7L))
                .thenReturn(Optional.of(new Usuario("ana@mail.com", "hash", Rol.DONANTE, 7L)));

        acceso.otorgar(7L, "Ana Perez", "ana@mail.com", false);

        verify(usuarioRepository, never()).save(any());
        verify(restTemplate, after(300).never()).postForObject(anyString(), any(), eq(Void.class));
    }
}
