package com.mcpgateway.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for POST /auth/register. orgId optional; roles default to USER. */
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private Long organizationId;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
}
