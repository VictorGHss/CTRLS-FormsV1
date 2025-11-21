package br.dev.ctrls.api.domain.clinic.repository;

import br.dev.ctrls.api.domain.clinic.Clinic;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório da entidade Clinic com suporte a busca por link público.
 */
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    Optional<Clinic> findByLinkUuid(UUID linkUuid);
}

