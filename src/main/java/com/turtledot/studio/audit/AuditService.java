package com.turtledot.studio.audit;

import com.turtledot.studio.audit.dto.AuditLogResponseDto;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.user.User;
import com.turtledot.studio.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long studioId, Long userId, String action, String entityType, Long entityId, String description, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog(studioId, userId, action, entityType, entityId, description, ipAddress);
            auditLogRepository.save(auditLog);
        } catch (Exception ignored) {
            // Never fail outer transaction if audit logging encounters an issue
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponseDto> getAuditLogs(UserPrincipal user, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByStudioId(user.getStudioId(), pageable);

        Map<Long, User> userMap = userRepository.findByStudioId(user.getStudioId()).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (existing, replacing) -> existing));

        List<AuditLogResponseDto> dtos = page.getContent().stream()
                .map(log -> {
                    User u = log.getUserId() != null ? userMap.get(log.getUserId()) : null;
                    String email = u != null ? u.getEmail() : "System";
                    String name = u != null ? u.getFullName() : "System";
                    return AuditLogResponseDto.fromEntity(log, email, name);
                })
                .collect(Collectors.toList());

        return new PageResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getRecentActivities(Long studioId) {
        List<AuditLog> logs = auditLogRepository.findTop10ByStudioIdOrderByCreatedAtDesc(studioId);
        Map<Long, User> userMap = userRepository.findByStudioId(studioId).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (existing, replacing) -> existing));

        return logs.stream()
                .map(log -> {
                    User u = log.getUserId() != null ? userMap.get(log.getUserId()) : null;
                    String email = u != null ? u.getEmail() : "System";
                    String name = u != null ? u.getFullName() : "System";
                    return AuditLogResponseDto.fromEntity(log, email, name);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getEmployeeActivities(Long userId) {
        List<AuditLog> logs = auditLogRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
        User u = userRepository.findById(userId).orElse(null);
        String email = u != null ? u.getEmail() : "Me";
        String name = u != null ? u.getFullName() : "Me";

        return logs.stream()
                .map(log -> AuditLogResponseDto.fromEntity(log, email, name))
                .collect(Collectors.toList());
    }
}
