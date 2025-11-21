package br.dev.ctrls.api.domain.clinic;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Estrutura complementar para tema visual da clínica.
 */
@Embeddable
public class ClinicTheme {

    @Column(name = "theme_logo_url")
    private String logoUrl;

    @Column(name = "theme_primary_color", length = 7)
    private String primaryColor;

    @Column(name = "theme_address")
    private String address;

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

