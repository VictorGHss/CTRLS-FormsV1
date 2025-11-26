package br.dev.ctrls.api.infrastructure.security;

import br.dev.ctrls.api.domain.clinic.repository.ClinicRepository;
import br.dev.ctrls.api.domain.user.repository.DoctorRepository;
import br.dev.ctrls.api.tenant.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter para validação de Multi-Tenancy com segurança reforçada.
 *
 * RESPONSABILIDADES:
 * 1. Validar header X-Clinic-ID
 * 2. Verificar se a clínica existe no banco
 * 3. Validar se o usuário autenticado tem vínculo com a clínica
 * 4. Armazenar clinic_id no ThreadLocal para uso nas queries
 *
 * SEGURANÇA CRÍTICA:
 * - ThreadLocal SEMPRE limpo no finally (evita memory leak em thread pools)
 * - Logs sanitizados (sem IDs sensíveis em produção)
 * - Performance: existsById em vez de findById
 * - Validação robusta de UUID
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Extrair header de tenant
            String clinicHeader = request.getHeader("X-Clinic-ID");

            // Se não há header, prosseguir sem validação (endpoints públicos)
            if (clinicHeader == null || clinicHeader.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            // Validar apenas se usuário estiver autenticado
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                filterChain.doFilter(request, response);
                return;
            }

            // Processar validação de tenant
            processTenantValidation(request, response, clinicHeader, authentication);

            // Continuar cadeia de filtros
            filterChain.doFilter(request, response);

        } finally {
            // ✅ CRÍTICO: SEMPRE limpar ThreadLocal (memory leak prevention)
            TenantContextHolder.clear();
            log.trace("🧹 [TenantFilter] ThreadLocal limpo");
        }
    }

    /**
     * Processa validação de tenant com tratamento de erros robusto.
     */
    private void processTenantValidation(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String clinicHeader,
                                        Authentication authentication) throws IOException {
        try {
            // Parse UUID com validação
            UUID clinicId = UUID.fromString(clinicHeader);
            String userIdStr = authentication.getName();
            UUID userId = UUID.fromString(userIdStr);

            log.debug("🔍 [TenantFilter] Validando acesso ao tenant");

            // 1. Verificar se clínica existe (Performance: existsById não carrega entidade)
            if (!clinicRepository.existsById(clinicId)) {
                log.warn("⚠️ [TenantFilter] Tentativa de acesso a clínica inexistente");
                sendErrorResponse(response, HttpStatus.BAD_REQUEST,
                                "Clínica inválida ou não encontrada");
                return;
            }

            // 2. Verificar vínculo usuário-clínica
            boolean isDoctorLinked = doctorRepository.existsByIdAndClinicsId(userId, clinicId);

            // 3. Verificar se é administrador global (bypass de validação)
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().contains("ADMIN"));

            if (!isDoctorLinked && !isAdmin) {
                log.warn("🚫 [TenantFilter] Acesso negado: usuário sem vínculo com tenant");
                sendErrorResponse(response, HttpStatus.FORBIDDEN,
                                "Acesso negado a este ambiente");
                return;
            }

            // 4. Armazenar tenant no ThreadLocal
            TenantContextHolder.setTenantId(clinicId.toString());
            log.debug("✅ [TenantFilter] Acesso validado com sucesso");

        } catch (IllegalArgumentException ex) {
            log.warn("⚠️ [TenantFilter] UUID inválido fornecido: {}", ex.getMessage());
            sendErrorResponse(response, HttpStatus.BAD_REQUEST,
                            "ID da clínica em formato inválido");
        }
    }

    /**
     * Envia resposta de erro no formato JSON (RFC 7807).
     */
    private void sendErrorResponse(HttpServletResponse response,
                                  HttpStatus status,
                                  String message) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(status.getReasonPhrase());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), problem);
    }
}