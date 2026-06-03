package com.mcpgateway.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tool_schemas")
public class ToolSchema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String toolName;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Lob
    @Column(nullable = false)
    private String schemaJson; // JSON schema definition

    private Long organizationId; // null means global/shared

    @Column(nullable = false)
    private Boolean enabled = true;

    public ToolSchema() {}

    public ToolSchema(String toolName, String title, String description, String schemaJson) {
        this.toolName = toolName;
        this.title = title;
        this.description = description;
        this.schemaJson = schemaJson;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSchemaJson() { return schemaJson; }
    public void setSchemaJson(String schemaJson) { this.schemaJson = schemaJson; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}

