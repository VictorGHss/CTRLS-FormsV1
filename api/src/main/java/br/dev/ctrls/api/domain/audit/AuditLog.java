package br.dev.ctrls.api.domain.audit;

import br.dev.ctrls.api.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Registro de auditoria de operações sensíveis.
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(name = "action", nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditScope scope;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
}
