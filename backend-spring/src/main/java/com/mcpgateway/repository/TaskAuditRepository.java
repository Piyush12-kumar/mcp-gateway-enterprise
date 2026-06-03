package com.mcpgateway.repository;

import com.mcpgateway.model.TaskAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskAuditRepository extends JpaRepository<TaskAudit, Long> {
    List<TaskAudit> findByUserId(Long userId);
    List<TaskAudit> findByOrganizationId(Long organizationId);
    List<TaskAudit> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
    List<TaskAudit> findByOrganizationIdAndToolName(Long organizationId, String toolName);
    
    @Query("SELECT ta FROM TaskAudit ta WHERE ta.organizationId = :orgId AND ta.createdAt >= :start AND ta.createdAt <= :end")
    List<TaskAudit> findAuditLog(@Param("orgId") Long orgId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

