package savemate.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio de negocio responsable de la creación, extracción y validación de tokens JWT.
 *
 * <p>Responsabilidad: Proveer utilidades para la gestión de tokens JWT (generación de access y refresh tokens,
 * extracción de claims, validación de expiración y extracción de información del usuario) usando una clave
 * HMAC configurada mediante la propiedad {@code jwt.secret}. Está diseñado para integrarse con la capa de
 * seguridad de Spring Security y con componentes que requieran información contenida en el token.
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Obtiene la clave de firma HMAC a partir del secreto configurado.
     *
     * @return instancia de {@link Key} utilizada para firmar y verificar tokens JWT
     */
    // Clave de firma segura
    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Extrae el nombre de usuario (subject) del token JWT.
     *
     * @param token token JWT del que se extraerá el subject
     * @return nombre de usuario contenido en el claim {@code sub} del token
     * @throws RuntimeException si el token no es válido o no se pueden extraer los claims
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha de expiración del token JWT.
     *
     * @param token token JWT del que se extraerá la fecha de expiración
     * @return instancia de {@link Date} con la fecha de expiración del token
     * @throws RuntimeException si el token no es válido o no se pueden extraer los claims
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim específico del token aplicando una función sobre los {@link Claims}.
     *
     * @param token          token JWT del que extraer los claims
     * @param claimsResolver función que obtiene el valor deseado a partir de {@link Claims}
     * @param <T>            tipo del valor extraído
     * @return el valor extraído por la función {@code claimsResolver}
     * @throws RuntimeException si el token no es válido o no se pueden extraer los claims
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae todos los claims contenidos en el token JWT verificando la firma.
     *
     * @param token token JWT a parsear
     * @return objeto {@link Claims} con todos los claims del token
     * @throws RuntimeException si ocurre cualquier error durante el parseo o la verificación del token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Error extrayendo claims del token: {}", e.getMessage());
            throw new RuntimeException("Token inválido");
        }
    }

    /**
     * Comprueba si el token JWT ha expirado.
     *
     * @param token token JWT a verificar
     * @return {@code true} si el token está expirado; {@code false} en caso contrario
     * @throws RuntimeException si el token no es válido o no se pueden extraer los claims
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Genera un token JWT (access token) para el usuario indicado sin claims adicionales.
     *
     * @param userDetails detalles del usuario para el que se genera el token
     * @return token JWT firmado como {@link String}
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Genera un token JWT (access token) incluyendo claims adicionales.
     *
     * @param extraClaims mapa de claims adicionales que se incluirán en el token
     * @param userDetails detalles del usuario (se usará {@code userDetails.getUsername()} como subject)
     * @return token JWT firmado como {@link String}
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Genera un refresh token JWT para el usuario indicado con una expiración mayor.
     *
     * @param userDetails detalles del usuario para el que se genera el refresh token
     * @return refresh token JWT firmado como {@link String}
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration * 3)) // 3 veces más tiempo
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida que el token JWT corresponda al usuario indicado y que no esté expirado.
     *
     * @param token       token JWT a validar
     * @param userDetails detalles del usuario contra el que se valida el token
     * @return {@code true} si el token es válido y pertenece al usuario; {@code false} en caso contrario
     */
    public Boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Error validando token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Valida que el token JWT no esté expirado.
     *
     * @param token token JWT a validar
     * @return {@code true} si el token no está expirado; {@code false} si está expirado o es inválido
     */
    public Boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Error validando token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae información relevante del usuario contenida en el token JWT.
     *
     * <p>Devuelve un mapa con clave {@code username} (subject), {@code issuedAt}, {@code expiration}
     * y cualquier claim adicional presente en el token (excluyendo {@code sub}, {@code iat} y {@code exp}).
     *
     * @param token token JWT del que se extraerá la información
     * @return mapa con la información del usuario extraída del token; en caso de error se devuelve un mapa vacío
     */
    public Map<String, Object> extractUserInfo(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", claims.getSubject());
            userInfo.put("issuedAt", claims.getIssuedAt());
            userInfo.put("expiration", claims.getExpiration());

            // Agregar claims adicionales si existen
            for (String key : claims.keySet()) {
                if (!key.equals("sub") && !key.equals("iat") && !key.equals("exp")) {
                    userInfo.put(key, claims.get(key));
                }
            }

            return userInfo;
        } catch (Exception e) {
            log.error("Error extrayendo información del usuario del token: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Obtiene el tiempo de expiración configurado para los tokens (en milisegundos).
     *
     * @return valor de la propiedad {@code jwt.expiration}
     */
    public long getJwtExpiration() {
        return jwtExpiration;
    }
}