package br.dev.ctrls.api.application.service.auth;

import br.dev.ctrls.api.domain.clinic.Clinic;
import br.dev.ctrls.api.domain.user.User;
import br.dev.ctrls.api.infrastructure.config.props.CtrlsProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por gerar e validar JWTs multi-tenant.
 */
@Service
public class JwtService {

    private final CtrlsProperties ctrlsProperties;
    private final Key signingKey;

    public JwtService(CtrlsProperties ctrlsProperties) {
        this.ctrlsProperties = ctrlsProperties;
        String secret = ctrlsProperties.getSecurity().getJwt().getSecret();
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(User user, Clinic clinic) {
        Instant now = Instant.now();
        long expiration = ctrlsProperties.getSecurity().getJwt().getExpirationMs();
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expiration)))
                .addClaims(Map.of(
                        "roles", user.getRole().name(),
                        "clinic_id", clinic.getId() != null ? clinic.getId().toString() : null,
                        "email", user.getEmail()))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
        return UUID.fromString(claims.getSubject());
    }

    public String extractEmail(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
        return claims.get("email", String.class);
    }
}
