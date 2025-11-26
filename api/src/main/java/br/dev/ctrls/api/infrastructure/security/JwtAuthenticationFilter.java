package br.dev.ctrls.api.infrastructure.security;

import br.dev.ctrls.api.application.service.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter para autenticação via JWT (JSON Web Token).
 *
 * RESPONSABILIDADES:
 * 1. Extrair token JWT do header Authorization
 * 2. Validar assinatura e expiração do token
 * 3. Extrair claims (userId, roles) do token
 * 4. Criar Authentication no SecurityContext do Spring
 *
 * SEGURANÇA:
 * - Logs sanitizados (sem tokens ou IDs em produção)
 * - Exceções tratadas adequadamente (delegadas ao FilterExceptionHandler)
 * - Validação robusta antes de setar autenticação
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. Verificar presença do header Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7); // Remove "Bearer "

        try {
            authenticateWithJwt(request, jwt);
        } catch (Exception ex) {
            // Log do erro SEM expor o token
            log.warn("⚠️ [JwtFilter] Falha ao processar token JWT: {}", ex.getMessage());
            // Exceção será capturada pelo FilterExceptionHandler se necessário
            // Por ora, apenas não autentica o usuário (deixa passar para retornar 401/403)
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Processa autenticação via JWT com validações robustas.
     */
    private void authenticateWithJwt(HttpServletRequest request, String jwt) {
        // Extrair userId do token (claim 'subject')
        String userId = jwtService.extractUsername(jwt);

        // 2. Validar se token é válido e usuário não está autenticado ainda
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            if (jwtService.isTokenValid(jwt)) {
                // Extrair roles do token
                List<String> roles = jwtService.extractRoles(jwt);
                var authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                // Criar token de autenticação do Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId,    // Principal (User ID)
                        null,      // Credentials (não necessário após autenticação)
                        authorities // Authorities/Roles
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 3. Registrar autenticação no SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("✅ [JwtFilter] Usuário autenticado com sucesso");
            } else {
                log.debug("⚠️ [JwtFilter] Token inválido ou expirado");
            }
        }
    }
}