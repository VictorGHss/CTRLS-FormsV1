package br.dev.ctrls.api.infrastructure.security;

import br.dev.ctrls.api.application.service.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userId;

        // 1. Verifica se tem header Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Remove "Bearer "

        try {
            userId = jwtService.extractUsername(jwt); // O nosso subject é o ID

            // 2. Se tem ID e ainda não está autenticado no contexto
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (jwtService.isTokenValid(jwt)) {
                    // Pega as roles do token
                    List<String> roles = jwtService.extractRoles(jwt);
                    var authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role)) // Adiciona prefixo ROLE_
                            .toList();

                    // Cria o objeto de autenticação do Spring (User ID + Roles)
                    // Usamos o userId como principal para não precisar ir no banco toda vez
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId, // Principal (String UUID)
                            null,   // Credentials
                            authorities // Authorities
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 3. Loga o usuário no contexto da requisição
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println("🔑 [JwtFilter] Usuário autenticado via Token: " + userId);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ [JwtFilter] Erro ao validar token: " + e.getMessage());
            // Não lançamos erro aqui, deixamos seguir para dar 403 lá na frente se necessário
        }

        filterChain.doFilter(request, response);
    }
}