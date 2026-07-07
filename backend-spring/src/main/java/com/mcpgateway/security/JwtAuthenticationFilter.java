package com.mcpgateway.security;

import com.mcpgateway.service.JwtService;
import com.mcpgateway.web.RequestContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates the JWT on each request. If valid:
 *  1. Populates Spring Security's SecurityContext (enables @PreAuthorize / hasRole).
 *  2. Bridges userId/orgId into the existing ThreadLocal RequestContext, so all
 *     current controllers that call RequestContext.getOrgId() keep working unchanged.
 * Requests without a valid token simply pass through unauthenticated; the security
 * filter chain then decides whether the target endpoint requires authentication.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                String username = claims.getSubject();
                Long userId = claims.get("userId", Number.class).longValue();
                Number orgNum = claims.get("orgId", Number.class);
                Long orgId = (orgNum == null || orgNum.longValue() < 0) ? null : orgNum.longValue();
                List<String> roles = claims.get("roles", List.class);

                var authorities = (roles == null ? Collections.<String>emptyList() : roles).stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .collect(Collectors.toList());

                var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Bridge to legacy RequestContext for existing tenant checks.
                RequestContext.setUserId(userId);
                RequestContext.setOrgId(orgId);
            } catch (Exception ignored) {
                // Invalid/expired token -> leave unauthenticated; chain will reject if needed.
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Prevent ThreadLocal leakage between pooled requests.
            RequestContext.clear();
        }
    }
}

