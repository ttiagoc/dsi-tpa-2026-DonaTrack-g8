package ar.edu.utn.frba.ddsi.donaciones.models.entities.usuarios;

import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.common.security.Rol;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.UsuarioRepository;

/**
 * Donantes que no se registran solos (los carga una persona administradora en el depósito o llegan por la
 * importación CSV): se les crea el usuario, con el correo como nombre de usuario y una contraseña temporal, y
 * se les manda la bienvenida con esas credenciales para que accedan por primera vez.
 */
@Component
public class AccesoInicialDonante {

    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LONGITUD_PASSWORD = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final RestDonacionesConfig config;

    @Autowired
    public AccesoInicialDonante(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            RestTemplate restTemplate, RestDonacionesConfig config) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
        this.config = config;
    }

    /**
     * Crea el usuario del donante si todavía no tiene uno y le avisa por correo. Si el donante es nuevo se le
     * da la bienvenida aunque no se haya podido crear el usuario (por ejemplo, porque el correo ya es el
     * nombre de usuario de otra cuenta); si ya existía, solo se le escribe cuando recibe credenciales nuevas.
     */
    public void otorgar(Long donanteId, String nombre, String email, boolean donanteNuevo) {
        if (email == null || email.isBlank()) {
            return;
        }
        String password = crearUsuarioSiNoTiene(donanteId, email);
        if (password == null && !donanteNuevo) {
            return;
        }
        notificarBienvenida(donanteId, nombre, email, password);
    }

    // devuelve la contraseña temporal del usuario creado, o null si no se creó
    private String crearUsuarioSiNoTiene(Long donanteId, String email) {
        if (usuarioRepository == null || passwordEncoder == null || donanteId == null) {
            return null;
        }
        try {
            if (usuarioRepository.buscarPorRolYPerfilId(Rol.DONANTE, donanteId).isPresent()
                    || usuarioRepository.existeUsername(email)) {
                return null;
            }
            String password = generarPassword();
            usuarioRepository.save(new Usuario(email, passwordEncoder.encode(password), Rol.DONANTE, donanteId));
            return password;
        } catch (Exception e) {
            System.err.println("No se pudo crear el usuario del donante " + donanteId + ": " + e.getMessage());
            return null;
        }
    }

    private void notificarBienvenida(Long donanteId, String nombre, String email, String password) {
        if (restTemplate == null || config == null) {
            return;
        }
        String saludo = "¡Bienvenido/a a DonaTrack" + (nombre != null && !nombre.isBlank() ? ", " + nombre.trim() : "") + "!";
        String mensaje = password == null
                ? saludo + " Tu cuenta de donante ya está registrada."
                : saludo + " Ya tenés tu cuenta de donante. Ingresá en " + urlLogin()
                        + " con el usuario " + Usuario.normalizar(email) + " y la contraseña temporal " + password + ".";
        NotificacionRequest request = new NotificacionRequest(
                email, TipoContacto.EMAIL, mensaje, "ALTA_USUARIO", "DONANTE", donanteId, null);
        String url = config.getNotificacionesUrl() + "/notificaciones";
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.postForObject(url, request, Void.class);
            } catch (Exception e) {
                System.err.println("No se pudo enviar la bienvenida al donante " + donanteId + ": " + e.getMessage());
            }
        });
    }

    private String urlLogin() {
        String base = config.getWebUrl() != null && !config.getWebUrl().isBlank()
                ? config.getWebUrl().replaceAll("/+$", "")
                : "http://localhost:7070";
        return base + "/login";
    }

    private static String generarPassword() {
        StringBuilder sb = new StringBuilder(LONGITUD_PASSWORD);
        for (int i = 0; i < LONGITUD_PASSWORD; i++) {
            sb.append(CARACTERES.charAt(RANDOM.nextInt(CARACTERES.length())));
        }
        return sb.toString();
    }
}
