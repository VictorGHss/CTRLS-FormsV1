package br.dev.ctrls.api.domain.user.repository;

import br.dev.ctrls.api.domain.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório base para a hierarquia de usuários.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
}

