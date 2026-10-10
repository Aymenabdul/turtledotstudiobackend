package com.turtledot.studio.dashboard;

import com.turtledot.studio.audit.AuditService;
import com.turtledot.studio.audit.dto.AuditLogResponseDto;
import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.dashboard.dto.DashboardStatsDto;
import com.turtledot.studio.dashboard.dto.MonthlyRevenueDto;
import com.turtledot.studio.event.dto.EventResponseDto;
import com.turtledot.studio.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Endpoints for studio analytics, revenue metrics, and operational overview")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuditService auditService;

    public DashboardController(DashboardService dashboardService, AuditService auditService) {
        this.dashboardService = dashboardService;
        this.auditService = auditService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get high-level studio KPI stats (Revenue, Pending, Clients, Storage)")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        DashboardStatsDto stats = dashboardService.getStudioStats(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/monthly-revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get monthly revenue trend for the past 6 months")
    public ResponseEntity<ApiResponse<List<MonthlyRevenueDto>>> getMonthlyRevenue(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<MonthlyRevenueDto> list = dashboardService.getMonthlyRevenue(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/upcoming-events")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get upcoming photo shoots and events")
    public ResponseEntity<ApiResponse<List<EventResponseDto>>> getUpcomingEvents(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<EventResponseDto> list = dashboardService.getUpcomingEvents(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get recent studio activity feed")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDto>>> getRecentActivity(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<AuditLogResponseDto> list = auditService.getRecentActivities(userPrincipal.getStudioId());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/employee-stats")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get employee dashboard overview stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmployeeStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Map<String, Object> stats = dashboardService.getEmployeeDashboardStats(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
