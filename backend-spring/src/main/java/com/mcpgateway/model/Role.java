package com.mcpgateway.model;

/**
 * Application roles used for Role-Based Access Control (RBAC).
 * - ADMIN: full platform access (manage any org/user).
 * - ORG_ADMIN: manage users/servers/schemas within their own organization.
 * - USER: can call tools and view their own data.
 */
public enum Role {
    ADMIN,
    ORG_ADMIN,
    USER
}
