package br.dev.ctrls.api.domain.clinic;

import br.dev.ctrls.api.domain.common.BaseEntity;
import br.dev.ctrls.api.domain.user.Doctor;
import br.dev.ctrls.api.infrastructure.persistence.converter.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Clínica (tenant raiz) que concentra a marca e o token Feegow.
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "clinics")
public class Clinic extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Pattern(regexp = "\\d{14}")
    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @NotBlank
    @Column(nullable = false)
    private String address;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "feegow_api_token", nullable = false, length = 512)
    private String feegowApiToken;

    @Column(name = "logo_url")
    private String logoUrl;

    @Pattern(regexp = "^#?[0-9a-fA-F]{6}$")
    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Builder.Default
    @Column(name = "link_uuid", nullable = false, unique = true)
    private UUID linkUuid = UUID.randomUUID();

    @Builder.Default
    @ManyToMany(mappedBy = "clinics", fetch = FetchType.LAZY)
    private Set<Doctor> doctors = new HashSet<>();
}
