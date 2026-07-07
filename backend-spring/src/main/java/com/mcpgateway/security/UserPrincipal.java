package com.mcpgateway.security;

import com.mcpgateway.model.Role;
import com.mcpgateway.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Wraps our JPA {@link User} so Spring Security can use it.
 * Also exposes userId/orgId so the JWT filter and controllers can read tenant info.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final Long orgId;
    private final String username;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.orgId = user.getOrganizationId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        // Spring expects authorities prefixed with ROLE_ for hasRole() checks.
        this.authorities = user.getRoles().stream()
                .map(Role::name)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
    }

    public Long getId() { return id; }
    public Long getOrgId() { return orgId; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}

