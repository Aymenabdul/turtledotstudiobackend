package com.turtledot.studio.audit;

import com.turtledot.studio.audit.dto.AuditLogResponseDto;
import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit Logs", description = "Endpoints for reviewing studio activity and security audit trail")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get paginated audit logs for the studio")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponseDto>>> getAuditLogs(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<AuditLogResponseDto> result = auditService.getAuditLogs(userPrincipal, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/my-activity")
    @Operation(summary = "Get recent activity performed by the current user (Employee or Admin)")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDto>>> getMyActivity(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<AuditLogResponseDto> result = auditService.getEmployeeActivities(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
