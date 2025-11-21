package br.dev.ctrls.api.application.service.auth;

import br.dev.ctrls.api.application.service.auth.dto.LoginRequest;
import br.dev.ctrls.api.application.service.auth.dto.LoginResponse;
import br.dev.ctrls.api.domain.clinic.Clinic;
import br.dev.ctrls.api.domain.clinic.repository.ClinicRepository;
import br.dev.ctrls.api.domain.user.User;
import br.dev.ctrls.api.domain.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Serviço de autenticação com validação de tenant.
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

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuário inválido"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        Clinic clinic = clinicRepository.findById(UUID.fromString(request.clinicId()))
                .orElseThrow(() -> new IllegalArgumentException("Clínica não encontrada"));

        String token = jwtService.generateToken(user, clinic);
        return new LoginResponse(token);
    }
}

