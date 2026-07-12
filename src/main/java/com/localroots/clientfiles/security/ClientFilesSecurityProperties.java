package com.localroots.clientfiles.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "client-files.security")
public class ClientFilesSecurityProperties {

    /**
     * Development bridge only. Production should resolve the tenant from an authenticated user/JWT.
     */
    private boolean allowTenantHeader = false;

    /**
     * Development bridge only. Keep false in production until contact ownership is checked against CRM data.
     */
    private boolean allowUnverifiedContactIds = false;

    /**
     * Development bridge only. Keep false in production until estimate ownership is checked.
     */
    private boolean allowUnverifiedEstimateIds = false;

    public boolean isAllowTenantHeader() {
        return allowTenantHeader;
    }

    public void setAllowTenantHeader(boolean allowTenantHeader) {
        this.allowTenantHeader = allowTenantHeader;
    }

    public boolean isAllowUnverifiedContactIds() {
        return allowUnverifiedContactIds;
    }

    public void setAllowUnverifiedContactIds(boolean allowUnverifiedContactIds) {
        this.allowUnverifiedContactIds = allowUnverifiedContactIds;
    }

    public boolean isAllowUnverifiedEstimateIds() {
        return allowUnverifiedEstimateIds;
    }

    public void setAllowUnverifiedEstimateIds(boolean allowUnverifiedEstimateIds) {
        this.allowUnverifiedEstimateIds = allowUnverifiedEstimateIds;
    }
}
