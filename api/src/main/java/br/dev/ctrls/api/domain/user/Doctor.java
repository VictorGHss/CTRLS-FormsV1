package br.dev.ctrls.api.domain.user;

import br.dev.ctrls.api.domain.clinic.Clinic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Médico vinculado a múltiplas clínicas e secretárias (relações M:N).
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "doctors")
public class Doctor extends User {

    @NotBlank
    @Column(nullable = false, length = 30)
    private String crm;

    @Pattern(regexp = "^[A-Z]{2}$")
    @Column(nullable = false, length = 2)
    private String uf;

    @Column(length = 1000)
    private String bio;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    /**
     * Relação M:N com clínicas: um médico atua em várias clínicas e vice-versa.
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "doctor_clinic",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "clinic_id"))
    private Set<Clinic> clinics = new HashSet<>();

    /**
     * Relação M:N com secretárias: cada secretária pode apoiar vários médicos.
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "doctor_secretary",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "secretary_id"))
    private Set<br.dev.ctrls.api.domain.secretary.Secretary> secretaries = new HashSet<>();

    @PrePersist
    void assignRole() {
        setRole(UserRole.DOCTOR);
    }
}
