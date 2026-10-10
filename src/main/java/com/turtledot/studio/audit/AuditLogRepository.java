package com.turtledot.studio.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByStudioId(Long studioId, Pageable pageable);

    List<AuditLog> findTop10ByStudioIdOrderByCreatedAtDesc(Long studioId);

    List<AuditLog> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
