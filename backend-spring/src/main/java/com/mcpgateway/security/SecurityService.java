package com.mcpgateway.security;

import com.mcpgateway.web.RequestContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Helper methods for Spring Security SpEL expressions in @PreAuthorize.
 * Example: @PreAuthorize("@securityService.canAccessOrg(#orgId)")
 */
@Service("securityService")
public class SecurityService {

    /**
     * Check if the current user can access resources for the given organization.
     * Admins can access any org; others can only access their own.
     */
    public boolean canAccessOrg(Long orgId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        // Admins can access everything
        if (auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }
        // Others can only access their own organization
        Long userOrgId = RequestContext.getOrgId();
        return orgId != null && orgId.equals(userOrgId);
    }

    /**
     * Check if the current user is an admin.
     */
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Check if the current user is at least org admin for their organization.
     */
    public boolean isOrgAdminOrHigher() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || 
                              a.getAuthority().equals("ROLE_ORG_ADMIN"));
    }
}

