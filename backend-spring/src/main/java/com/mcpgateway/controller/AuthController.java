package com.mcpgateway.controller;

import com.mcpgateway.dto.LoginRequest;
import com.mcpgateway.dto.RegisterRequest;
import com.mcpgateway.model.Role;
import com.mcpgateway.model.User;
import com.mcpgateway.repository.UserRepository;
import com.mcpgateway.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Public authentication endpoints.
 * POST /auth/register -> create a user with a hashed password (defaults to USER role).
 * POST /auth/login    -> verify credentials and return a signed JWT.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authManager, UserRepository users,
                          PasswordEncoder encoder, JwtService jwtService) {
        this.authManager = authManager;
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (users.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username_already_exists"));
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPasswordHash(encoder.encode(req.getPassword()));
        u.setOrganizationId(req.getOrganizationId());
        u.setRoles(Set.of(Role.USER));
        User saved = users.save(u);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "username", saved.getUsername(),
                "organizationId", saved.getOrganizationId() == null ? "" : saved.getOrganizationId(),
                "roles", saved.getRoles()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }
        User u = users.findByUsername(req.getUsername()).orElseThrow();
        List<String> roles = u.getRoles().stream().map(Role::name).toList();
        String token = jwtService.generateToken(u.getUsername(), u.getId(), u.getOrganizationId(), roles);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", u.getId(),
                "organizationId", u.getOrganizationId() == null ? "" : u.getOrganizationId(),
                "roles", roles));
    }
}
