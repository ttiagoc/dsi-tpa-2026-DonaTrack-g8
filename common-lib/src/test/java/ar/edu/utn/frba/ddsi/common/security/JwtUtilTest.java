package ar.edu.utn.frba.ddsi.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.exceptions.NoAutenticadoException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@DisplayName("Tests de JwtUtil")
class JwtUtilTest {

    private static final String SECRETO = "secreto-de-test-con-al-menos-32-bytes-0123";
    private final JwtUtil jwtUtil = new JwtUtil(SECRETO);

    @Test
    @DisplayName("El access token conserva usuario, rol y perfil")
    void accessTokenConservaLosClaims() {
        UsuarioAutenticado ana = new UsuarioAutenticado(7L, "ana", Rol.DONANTE, 42L);

        UsuarioAutenticado leido = jwtUtil.leerAccessToken(jwtUtil.generarAccessToken(ana));

        assertEquals(ana, leido);
    }

    @Test
    @DisplayName("El administrador no tiene perfil")
    void administradorSinPerfil() {
        UsuarioAutenticado admin = new UsuarioAutenticado(1L, "admin", Rol.ADMINISTRADOR, null);

        assertNull(jwtUtil.leerAccessToken(jwtUtil.generarAccessToken(admin)).perfilId());
    }

    @Test
    @DisplayName("Un refresh token no sirve como access token, ni al reves")
    void noSeMezclanLosTiposDeToken() {
        String refresh = jwtUtil.generarRefreshToken("ana");
        String access = jwtUtil.generarAccessToken(new UsuarioAutenticado(7L, "ana", Rol.DONANTE, 42L));

        assertEquals("ana", jwtUtil.leerRefreshToken(refresh));
        assertThrows(NoAutenticadoException.class, () -> jwtUtil.leerAccessToken(refresh));
        assertThrows(NoAutenticadoException.class, () -> jwtUtil.leerRefreshToken(access));
    }

    @Test
    @DisplayName("Rechaza tokens firmados con otro secreto")
    void rechazaFirmaAjena() {
        JwtUtil otro = new JwtUtil("otro-secreto-distinto-con-mas-de-32-bytes-99");
        String token = otro.generarAccessToken(new UsuarioAutenticado(7L, "ana", Rol.ADMINISTRADOR, null));

        assertThrows(NoAutenticadoException.class, () -> jwtUtil.leerAccessToken(token));
    }

    @Test
    @DisplayName("Rechaza tokens vencidos y basura")
    void rechazaVencidosYBasura() {
        String vencido = Jwts.builder()
                .subject("ana")
                .issuer("donatrack")
                .claim(JwtUtil.CLAIM_ROL, "DONANTE")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(NoAutenticadoException.class, () -> jwtUtil.leerAccessToken(vencido));
        assertThrows(NoAutenticadoException.class, () -> jwtUtil.leerAccessToken("no-es-un-token"));
    }
}
