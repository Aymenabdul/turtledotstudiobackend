package com.turtledot.studio.employee;

import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.employee.dto.*;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.user.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Endpoints for managing studio employees, client assignments, and permissions")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @Operation(summary = "Get paginated employees list (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponseDto>>> getEmployees(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        PageResponse<EmployeeResponseDto> response = employeeService.getEmployees(userPrincipal, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current logged-in employee profile and permissions")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> getCurrentEmployee(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        EmployeeResponseDto response = employeeService.getCurrentEmployee(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee profile by ID")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> getEmployeeById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        EmployeeResponseDto response = employeeService.getEmployeeById(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "Create a new employee with login credentials and permissions (Admin only)")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> createEmployee(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody EmployeeCreateRequest request
    ) {
        EmployeeResponseDto created = employeeService.createEmployee(userPrincipal, request);
        return ResponseEntity.ok(ApiResponse.success("Employee created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee profile (Admin only)")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> updateEmployee(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request
    ) {
        EmployeeResponseDto updated = employeeService.updateEmployee(userPrincipal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee profile updated", updated));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable an employee account (Admin only)")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestParam UserStatus status
    ) {
        employeeService.toggleEmployeeStatus(userPrincipal, id, status);
        return ResponseEntity.ok(ApiResponse.success("Employee status updated to " + status, null));
    }

    @PostMapping("/{id}/clients")
    @Operation(summary = "Assign or update client access list for an employee (Admin only)")
    public ResponseEntity<ApiResponse<Void>> assignClients(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody AssignClientsRequest request
    ) {
        employeeService.assignClients(userPrincipal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Client assignments updated successfully", null));
    }

    @PostMapping("/{id}/permissions")
    @Operation(summary = "Assign or update permission set for an employee (Admin only)")
    public ResponseEntity<ApiResponse<Void>> assignPermissions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody AssignPermissionsRequest request
    ) {
        employeeService.assignPermissions(userPrincipal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee permissions updated successfully", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee profile and user credentials (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        employeeService.deleteEmployee(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted successfully", null));
    }
}
