package br.dev.ctrls.api.domain.secretary;

import br.dev.ctrls.api.domain.user.Doctor;
import br.dev.ctrls.api.domain.user.User;
import br.dev.ctrls.api.domain.user.UserRole;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Representa secretárias que podem atuar para múltiplos médicos.
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "secretaries")
public class Secretary extends User {

    @Default
    @ManyToMany(mappedBy = "secretaries", fetch = FetchType.LAZY)
    private Set<Doctor> doctors = new HashSet<>();

    @PrePersist
    void assignRole() {
        setRole(UserRole.SECRETARY);
    }
}
