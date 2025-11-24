package br.dev.ctrls.api.infrastructure.bootstrap;

import br.dev.ctrls.api.application.service.auth.JwtService;
import br.dev.ctrls.api.domain.clinic.Clinic;
import br.dev.ctrls.api.domain.clinic.repository.ClinicRepository;
import br.dev.ctrls.api.domain.form.FormTemplate;
import br.dev.ctrls.api.domain.form.repository.FormTemplateRepository;
import br.dev.ctrls.api.domain.user.Doctor;
import br.dev.ctrls.api.domain.user.User;
import br.dev.ctrls.api.domain.user.UserRole;
import br.dev.ctrls.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.UUID;

/**
 * Cria dados básicos para desenvolvimento local.
 */
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final FormTemplateRepository formTemplateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public void run(String... args) {
        if (clinicRepository.count() > 0) {
            return;
        }

        Clinic clinic = Clinic.builder()
                .name("Clínica Inovare")
                .cnpj("27358290000110")
                .address("Av. Paulista, 1000 - São Paulo")
                .feegowApiToken("MOCK_FEEGOW_TOKEN")
                .logoUrl("https://placehold.co/160x60")
                .primaryColor("#0062FF")
                .build();
        clinicRepository.save(clinic);

        Doctor doctor = Doctor.builder()
                .email("victor@ctrls.dev")
                .passwordHash(passwordEncoder.encode("password"))
                .name("Dr. Victor")
                .phone("11999998888")
                .crm("12345")
                .uf("SP")
                .bio("Cardiologista focado em medicina preventiva.")
                .role(UserRole.DOCTOR)
                .clinics(new HashSet<>())
                .build();
        doctor.getClinics().add(clinic);
        userRepository.save(doctor);

        User admin = User.builder()
                .email("admin@ctrls.dev")
                .passwordHash(passwordEncoder.encode("password"))
                .name("Admin CTRLS")
                .phone("11911112222")
                .role(UserRole.SUPER_ADMIN)
                .build();
        userRepository.save(admin);

        FormTemplate template = FormTemplate.builder()
                .title("Anamnese Geral")
                .description("Coleta padrão de informações clínicas")
                .schemaJson("[{\"id\":\"q1\",\"type\":\"text\",\"label\":\"Queixa Principal\",\"required\":true}]")
                .publicUuid(UUID.randomUUID())
                .active(true)
                .clinic(clinic)
                .doctor(doctor)
                .build();
        formTemplateRepository.save(template);

        String formLink = "http://localhost:3000/forms/" + template.getPublicUuid();
        String jwt = jwtService.generateToken(doctor, clinic);

        System.out.println("=== DEV DATA SEEDER ===");
        System.out.println("Generated Form Link: " + formLink);
        System.out.println("Admin Credentials: admin@ctrls.dev / password");
        System.out.println("Doctor JWT: " + jwt);
    }
}
