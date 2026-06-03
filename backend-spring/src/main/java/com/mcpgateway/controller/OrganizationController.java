package com.mcpgateway.controller;

import com.mcpgateway.model.Organization;
import com.mcpgateway.repository.OrganizationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/organizations")
public class OrganizationController {
    private final OrganizationRepository orgRepo;

    public OrganizationController(OrganizationRepository orgRepo) {
        this.orgRepo = orgRepo;
    }

    @PostMapping
    public ResponseEntity<?> createOrganization(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name required"));
        }
        
        if (orgRepo.findByName(name).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "organization_already_exists"));
        }
        
        Organization org = new Organization();
        org.setName(name);
        Organization saved = orgRepo.save(org);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "name", saved.getName()));
    }

    @GetMapping
    public ResponseEntity<?> listOrganizations() {
        List<Organization> orgs = orgRepo.findAll();
        return ResponseEntity.ok(orgs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrganization(@PathVariable Long id) {
        return orgRepo.findById(id)
            .map(org -> ResponseEntity.ok((Object) Map.of("id", org.getId(), "name", org.getName())))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name required"));
        }
        
        return orgRepo.findById(id)
            .map(org -> {
                org.setName(name);
                Organization saved = orgRepo.save(org);
                return ResponseEntity.ok((Object) Map.of("id", saved.getId(), "name", saved.getName()));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrganization(@PathVariable Long id) {
        if (!orgRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        orgRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}

