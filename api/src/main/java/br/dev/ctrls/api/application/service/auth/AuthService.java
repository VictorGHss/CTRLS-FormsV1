package br.dev.ctrls.api.application.service.auth;

import br.dev.ctrls.api.application.service.auth.dto.LoginRequest;
import br.dev.ctrls.api.application.service.auth.dto.LoginResponse;
import br.dev.ctrls.api.domain.clinic.Clinic;
import br.dev.ctrls.api.domain.clinic.repository.ClinicRepository;
import br.dev.ctrls.api.domain.user.User;
import br.dev.ctrls.api.domain.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Serviço de autenticação com validação de tenant.
 *
 * SECURITY: Valida no nível do banco de dados se o usuário pertence à clínica.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       ClinicRepository clinicRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.clinicRepository = clinicRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Autentica um usuário e gera token JWT com contexto de tenant (clínica).
     *
     * VALIDAÇÕES:
     * 1. Usuário existe e credenciais são válidas
     * 2. Clínica existe
     * 3. Usuário tem permissão para acessar a clínica (validado no DB)
     *
     * @param request Dados de login (email, senha, clinicId)
     * @return Token JWT com claims de usuário e tenant
     * @throws IllegalArgumentException se credenciais inválidas ou usuário não autorizado
     */
    public LoginResponse login(LoginRequest request) {
        UUID clinicId = UUID.fromString(request.clinicId());

        // Validar existência da clínica primeiro
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Clínica não encontrada"));

        // Buscar usuário COM validação de vínculo à clínica (query otimizada)
        User user = userRepository.findByEmailAndClinicId(request.email(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Credenciais inválidas ou usuário não autorizado para esta clínica"
                ));

        // Validar senha
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        // Gerar token com contexto de tenant
        String token = jwtService.generateToken(user, clinic);
        return new LoginResponse(token);
    }
}



