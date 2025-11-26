package br.dev.ctrls.api.domain.user.repository;

import br.dev.ctrls.api.domain.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório base para a hierarquia de usuários.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    /**
     * Busca um usuário por email validando se ele pertence à clínica especificada.
     *
     * REGRAS:
     * - SUPER_ADMIN: pode acessar qualquer clínica
     * - CLINIC_ADMIN: pode acessar qualquer clínica (gestão multi-tenant)
     * - DOCTOR: deve estar vinculado à clínica (via tabela doctor_clinic)
     * - SECRETARY: deve estar vinculado a um médico que atua na clínica
     *
     * @param email Email do usuário
     * @param clinicId ID da clínica
     * @return Optional contendo o usuário se válido para a clínica
     */
    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN Doctor d ON u.id = d.id
        LEFT JOIN d.clinics c
        LEFT JOIN Secretary s ON u.id = s.id
        LEFT JOIN s.doctors doc
        LEFT JOIN doc.clinics sc
        WHERE u.email = :email
        AND (
            u.role = 'SUPER_ADMIN'
            OR u.role = 'CLINIC_ADMIN'
            OR (u.role = 'DOCTOR' AND c.id = :clinicId)
            OR (u.role = 'SECRETARY' AND sc.id = :clinicId)
        )
    """)
    Optional<User> findByEmailAndClinicId(@Param("email") String email, @Param("clinicId") UUID clinicId);
}



