package com.mcpgateway.repository;

import com.mcpgateway.model.McpServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface McpServerRepository extends JpaRepository<McpServer, Long> {
    List<McpServer> findByOrganizationId(Long orgId);
}
