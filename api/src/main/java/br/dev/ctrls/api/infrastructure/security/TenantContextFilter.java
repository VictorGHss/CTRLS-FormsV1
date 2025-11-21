package br.dev.ctrls.api.infrastructure.security;

import br.dev.ctrls.api.domain.clinic.repository.ClinicRepository;
import br.dev.ctrls.api.domain.user.repository.DoctorRepository;
import br.dev.ctrls.api.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;

    public TenantContextFilter(ClinicRepository clinicRepository, DoctorRepository doctorRepository) {
        this.clinicRepository = clinicRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String clinicHeader = request.getHeader("X-Clinic-ID");

        // Se não tem header, o filtro não faz nada e deixa passar (endpoints públicos ou admin global)
        if (clinicHeader == null || clinicHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Só valida se o usuário estiver logado
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                UUID clinicId = UUID.fromString(clinicHeader);
                String userIdStr = authentication.getName(); // No JWT, o 'sub' é o UUID do usuário
                UUID userId = UUID.fromString(userIdStr);

                System.out.println("🔍 [TenantFilter] Verificando Acesso: User=" + userId + " -> Clinic=" + clinicId);

                // 1. Clínica existe?
                if (!clinicRepository.existsById(clinicId)) {
                    System.out.println("❌ [TenantFilter] Clínica não encontrada no banco.");
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Clínica inválida");
                    return;
                }

                // 2. Verifica Vínculo (Lógica Robusta: ID com ID)
                // Verificamos se existe um médico com esse ID vinculado a essa clínica
                boolean isDoctorLinked = doctorRepository.existsByIdAndClinicsId(userId, clinicId);

                // Se não for médico vinculado, verificamos se é Admin (Opcional, mas bom para testes)
                // Por enquanto, se não for médico da clínica, bloqueia.
                // (A menos que você tenha um usuário ADMIN global, ai precisaria liberar ele aqui)

                // Hack para permitir o Admin Global passar (se o ID bater com o admin)
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().contains("ADMIN"));

                if (!isDoctorLinked && !isAdmin) {
                    System.out.println("🚫 [TenantFilter] Acesso Negado: Usuário não vinculado à clínica.");
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado a este ambiente");
                    return;
                }

                // 3. Sucesso!
                System.out.println("✅ [TenantFilter] Acesso Permitido.");
                TenantContextHolder.setTenantId(clinicId.toString());

            } catch (IllegalArgumentException e) {
                System.out.println("❌ [TenantFilter] UUID Inválido: " + e.getMessage());
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID inválido");
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}