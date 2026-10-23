package ar.edu.utn.frba.ddsi.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.exceptions.NoAutenticadoException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Genera y valida los JWT. El access token lleva el rol y el perfil del usuario como claims, asi cualquier
 * servicio que conozca el secreto sabe quien llama sin consultar la base. El refresh token solo sirve
 * para pedir un access token nuevo.
 */
@Component
public class JwtUtil {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_USUARIO_ID = "usuarioId";
    public static final String CLAIM_PERFIL_ID = "perfilId";
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_REFRESH = "refresh";

    private static final String ISSUER = "donatrack";
    private static final long ACCESS_TOKEN_VALIDITY = 15 * 60 * 1000L; // 15 min
    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000L; // 7 dias

    private final SecretKey key;

    // HS256 exige un secreto de al menos 32 bytes; si es mas corto, Keys.hmacShaKeyFor falla al arrancar
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarAccessToken(UsuarioAutenticado usuario) {
        return Jwts.builder()
                .subject(usuario.username())
                .issuer(ISSUER)
                .claim(CLAIM_ROL, usuario.rol().name())
                .claim(CLAIM_USUARIO_ID, usuario.usuarioId())
                .claim(CLAIM_PERFIL_ID, usuario.perfilId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }

    public String generarRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuer(ISSUER)
                .claim(CLAIM_TYPE, TYPE_REFRESH) // diferenciamos refresh del access
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }

    /** Valida un access token y devuelve quien es el usuario. Un refresh token no se acepta como access. */
    public UsuarioAutenticado leerAccessToken(String token) {
        Claims claims = parsearClaims(token);
        if (TYPE_REFRESH.equals(claims.get(CLAIM_TYPE))) {
            throw new NoAutenticadoException("Token invalido");
        }
        try {
            return new UsuarioAutenticado(
                    aLong(claims.get(CLAIM_USUARIO_ID)),
                    claims.getSubject(),
                    Rol.valueOf(claims.get(CLAIM_ROL, String.class)),
                    aLong(claims.get(CLAIM_PERFIL_ID)));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new NoAutenticadoException("Token invalido");
        }
    }

    /** Valida un refresh token y devuelve el username al que pertenece. */
    public String leerRefreshToken(String token) {
        Claims claims = parsearClaims(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE))) {
            throw new NoAutenticadoException("Refresh token invalido");
        }
        return claims.getSubject();
    }

    public Claims parsearClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new NoAutenticadoException("Token invalido o vencido");
        }
    }

    private Long aLong(Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }
}
