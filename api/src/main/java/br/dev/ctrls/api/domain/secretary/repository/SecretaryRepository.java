package br.dev.ctrls.api.domain.secretary.repository;

import br.dev.ctrls.api.domain.secretary.Secretary;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de secretárias para consultas e cadastros.
 */
public interface SecretaryRepository extends JpaRepository<Secretary, UUID> {
}

