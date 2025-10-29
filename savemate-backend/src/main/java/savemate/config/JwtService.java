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

@Service
@Slf4j
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    // Clave de firma segura
    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    /**
     * Extrae el username (email) del token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extrae la fecha de expiración del token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extrae un claim específico del token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extrae todos los claims del token
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
     * Verifica si el token ha expirado
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * Genera un token para un usuario
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }
    
    /**
     * Genera un token con claims adicionales
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
     * Genera un token de refresco
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
     * Verifica si un token es válido para un usuario
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
     * Verifica si un token es válido (sin verificar usuario específico)
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
     * Extrae información del usuario del token
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
     * Obtiene el tiempo de expiración en milisegundos
     */
    public long getJwtExpiration() {
        return jwtExpiration;
    }
}