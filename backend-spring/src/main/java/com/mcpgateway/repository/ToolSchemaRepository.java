package com.mcpgateway.repository;

import com.mcpgateway.model.ToolSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolSchemaRepository extends JpaRepository<ToolSchema, Long> {
    Optional<ToolSchema> findByToolName(String toolName);
    List<ToolSchema> findByEnabled(Boolean enabled);
    List<ToolSchema> findByOrganizationIdOrOrganizationIdIsNull(Long orgId);
}

