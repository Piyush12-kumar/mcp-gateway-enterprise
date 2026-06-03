package com.mcpgateway.controller;

import com.mcpgateway.model.ToolSchema;
import com.mcpgateway.repository.ToolSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/schema")
public class SchemaController {
    private final ToolSchemaRepository schemaRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SchemaController(ToolSchemaRepository schemaRepo) {
        this.schemaRepo = schemaRepo;
    }

    @GetMapping(path = "/{tool}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getSchema(@PathVariable String tool) {
        return schemaRepo.findByToolName(tool)
            .map(schema -> {
                try {
                    Object schemaObj = objectMapper.readValue(schema.getSchemaJson(), Object.class);
                    return ResponseEntity.ok(schemaObj);
                } catch (Exception e) {
                    return ResponseEntity.ok((Object) Map.of(
                        "title", schema.getTitle(),
                        "description", schema.getDescription(),
                        "fields", List.of()
                    ));
                }
            })
            .orElseGet(() -> {
                // Return a default schema if not found
                String defaultSchema = "{\"title\":\"" + tool + "\",\"description\":\"Tool: " + tool + "\",\"fields\":[{\"name\":\"input\",\"type\":\"text\",\"label\":\"Input\"}]}";
                try {
                    return ResponseEntity.ok(objectMapper.readValue(defaultSchema, Object.class));
                } catch (Exception e) {
                    return ResponseEntity.ok(defaultSchema);
                }
            });
    }

    @GetMapping(path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllSchemas() {
        List<ToolSchema> schemas = schemaRepo.findByEnabled(true);
        return ResponseEntity.ok(schemas.stream().map(s -> Map.of(
            "toolName", s.getToolName(),
            "title", s.getTitle(),
            "description", s.getDescription()
        )).toList());
    }

    @PostMapping
    public ResponseEntity<?> createSchema(@RequestBody Map<String, String> body) {
        String toolName = body.get("toolName");
        String title = body.get("title");
        String description = body.get("description");
        String schemaJson = body.get("schemaJson");

        if (toolName == null || title == null || schemaJson == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "toolName, title, and schemaJson required"));
        }

        if (schemaRepo.findByToolName(toolName).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "schema_already_exists"));
        }

        ToolSchema schema = new ToolSchema(toolName, title, description != null ? description : "", schemaJson);
        Long orgId = com.mcpgateway.web.RequestContext.getOrgId();
        if (orgId != null) schema.setOrganizationId(orgId);
        
        ToolSchema saved = schemaRepo.save(schema);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "toolName", saved.getToolName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSchema(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return schemaRepo.findById(id)
            .map(schema -> {
                String title = body.get("title");
                String description = body.get("description");
                String schemaJson = body.get("schemaJson");
                Boolean enabled = body.containsKey("enabled") ? Boolean.parseBoolean(body.get("enabled")) : null;

                if (title != null) schema.setTitle(title);
                if (description != null) schema.setDescription(description);
                if (schemaJson != null) schema.setSchemaJson(schemaJson);
                if (enabled != null) schema.setEnabled(enabled);

                ToolSchema saved = schemaRepo.save(schema);
                return ResponseEntity.ok((Object) Map.of("id", saved.getId(), "toolName", saved.getToolName()));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSchema(@PathVariable Long id) {
        if (!schemaRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        schemaRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
