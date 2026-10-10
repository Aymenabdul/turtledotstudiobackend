package com.turtledot.studio.security;

import com.turtledot.studio.exception.ForbiddenException;
import com.turtledot.studio.user.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final JdbcTemplate jdbcTemplate;

    public AuthorizationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void checkStudioAccess(UserPrincipal user, Long studioId) {
        if (user == null || !user.getStudioId().equals(studioId)) {
            throw new ForbiddenException("You do not have access to resources in this studio");
        }
    }

    public void checkClientAccess(UserPrincipal user, Long clientId, String requiredPermission) {
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }

        // Admin has full access to all studio resources
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (user.getRole() == Role.CLIENT) {
            // Check if client owns this client profile
            Integer match = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM clients WHERE id = ? AND user_id = ? AND studio_id = ?",
                    Integer.class,
                    clientId, user.getId(), user.getStudioId()
            );
            if (match == null || match == 0) {
                throw new ForbiddenException("You can only access your own client data");
            }
            return;
        }

        if (user.getRole() == Role.EMPLOYEE) {
            // 1. Get employee ID
            Long employeeId = getEmployeeIdForUser(user.getId());
            if (employeeId == null) {
                throw new ForbiddenException("Employee record not found");
            }

            // 2. Check if employee has the required permission
            if (requiredPermission != null) {
                Integer permCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM employee_permissions WHERE employee_id = ? AND permission = ?",
                        Integer.class,
                        employeeId, requiredPermission
                );
                if (permCount == null || permCount == 0) {
                    throw new ForbiddenException("You do not have the '" + requiredPermission + "' permission");
                }
            }

            // 3. Check if employee is assigned to this client
            Integer accessCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM employee_client_access WHERE employee_id = ? AND client_id = ?",
                    Integer.class,
                    employeeId, clientId
            );
            if (accessCount == null || accessCount == 0) {
                throw new ForbiddenException("You are not assigned to this client");
            }
            return;
        }

        throw new ForbiddenException("Access denied");
    }

    public void checkEventAccess(UserPrincipal user, Long eventId, String requiredPermission) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        Long clientId = jdbcTemplate.queryForObject(
                "SELECT client_id FROM events WHERE id = ? AND studio_id = ?",
                Long.class,
                eventId, user.getStudioId()
        );

        if (clientId == null) {
            throw new ForbiddenException("Event not found or not in your studio");
        }

        checkClientAccess(user, clientId, requiredPermission);
    }

    public Long getEmployeeIdForUser(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM employees WHERE user_id = ?",
                    Long.class,
                    userId
            );
        } catch (Exception e) {
            return null;
        }
    }
}
