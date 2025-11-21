package br.dev.ctrls.api.domain.user.repository;

import br.dev.ctrls.api.domain.user.Doctor;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório dedicado aos médicos.
 */
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    boolean existsByIdAndClinicsId(UUID doctorId, UUID clinicId);
}
