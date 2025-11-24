package br.dev.ctrls.api.application.service.auth;

import br.dev.ctrls.api.domain.clinic.Clinic;
import br.dev.ctrls.api.domain.user.User;
import br.dev.ctrls.api.infrastructure.config.props.CtrlsProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List<String> extractRoles(String token) {
        final Claims claims = extractAllClaims(token);
        // O token grava como String única (ex: "DOCTOR"), convertemos para Lista
        Object roles = claims.get("roles");
        return roles != null ? List.of(roles.toString()) : List.of();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}