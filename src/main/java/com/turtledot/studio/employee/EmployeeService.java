package com.turtledot.studio.employee;

import com.turtledot.studio.audit.AuditService;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.employee.dto.*;
import com.turtledot.studio.exception.DuplicateResourceException;
import com.turtledot.studio.exception.ForbiddenException;
import com.turtledot.studio.exception.ResourceNotFoundException;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.user.Role;
import com.turtledot.studio.user.User;
import com.turtledot.studio.user.UserRepository;
import com.turtledot.studio.user.UserStatus;
import com.turtledot.studio.client.ClientRepository;
import com.turtledot.studio.client.ClientStatus;
import com.turtledot.studio.notification.EmailService;
import com.turtledot.studio.studio.Studio;
import com.turtledot.studio.studio.StudioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.turtledot.studio.event.Event;
import com.turtledot.studio.event.EventRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeClientAccessRepository accessRepository;
    private final EmployeePermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientRepository clientRepository;
    private final AuditService auditService;
    private final StudioRepository studioRepository;
    private final EmailService emailService;
    private final EventRepository eventRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           EmployeeClientAccessRepository accessRepository,
                           EmployeePermissionRepository permissionRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           ClientRepository clientRepository,
                           AuditService auditService,
                           StudioRepository studioRepository,
                           EmailService emailService,
                           EventRepository eventRepository) {
        this.employeeRepository = employeeRepository;
        this.accessRepository = accessRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clientRepository = clientRepository;
        this.auditService = auditService;
        this.studioRepository = studioRepository;
        this.emailService = emailService;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponseDto> getEmployees(UserPrincipal user, Pageable pageable) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can list employees");
        }

        Page<Employee> page = employeeRepository.findByStudioId(user.getStudioId(), pageable);
        List<EmployeeResponseDto> dtos = page.getContent().stream()
                .map(this::enrichEmployeeDto)
                .collect(Collectors.toList());

        return new PageResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDto getEmployeeById(UserPrincipal user, Long id) {
        Employee employee = employeeRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (user.getRole() != Role.ADMIN && !employee.getUserId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this employee profile");
        }

        return enrichEmployeeDto(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDto getCurrentEmployee(UserPrincipal user) {
        if (user.getRole() == Role.ADMIN) {
            EmployeeResponseDto adminDto = new EmployeeResponseDto();
            adminDto.setUserId(user.getId());
            adminDto.setFullName(user.getFullName());
            adminDto.setEmail(user.getEmail());
            adminDto.setPermissions(Arrays.asList(
                    "CLIENT_VIEW", "CLIENT_EDIT", "ALBUM_CREATE",
                    "PHOTO_UPLOAD", "PHOTO_DELETE", "VIDEO_UPLOAD",
                    "MEDIA_DOWNLOAD", "ALBUM_DELETE", "SHARE_MANAGE"
            ));
            return adminDto;
        }

        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        return enrichEmployeeDto(employee);
    }

    @Transactional
    public EmployeeResponseDto createEmployee(UserPrincipal user, EmployeeCreateRequest request) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can create employees");
        }

        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        // 1. Create User account
        User empUser = new User(
                user.getStudioId(),
                request.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhone(),
                Role.EMPLOYEE,
                UserStatus.ACTIVE
        );
        User savedUser = userRepository.save(empUser);

        // 2. Create Employee profile
        String employeeCode = generateEmployeeCode(user.getStudioId());
        Employee employee = new Employee(
                user.getStudioId(),
                savedUser.getId(),
                employeeCode,
                request.getDesignation() != null ? request.getDesignation() : "Staff Photographer",
                request.getDepartment() != null ? request.getDepartment() : "Photography",
                request.getNotes()
        );
        Employee savedEmployee = employeeRepository.save(employee);

        // 3. Assign Permissions
        List<String> perms = request.getPermissions();
        if (perms == null || perms.isEmpty()) {
            perms = Arrays.asList(
                    "CLIENT_VIEW", "PHOTO_UPLOAD", "PHOTO_DELETE",
                    "VIDEO_UPLOAD", "ALBUM_CREATE", "ALBUM_EDIT",
                    "MEDIA_DOWNLOAD", "CLIENT_EDIT"
            );
        }
        for (String perm : perms) {
            permissionRepository.save(new EmployeePermission(user.getStudioId(), savedEmployee.getId(), perm));
        }

        // Trigger Welcome Email to new employee
        Studio studio = studioRepository.findById(user.getStudioId()).orElse(null);
        emailService.sendEmployeeWelcomeEmail(studio, savedUser, savedEmployee, request.getPassword());

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "CREATE_EMPLOYEE",
                "EMPLOYEE",
                savedEmployee.getId(),
                "Created employee account '" + savedUser.getFullName() + "' (" + savedEmployee.getEmployeeCode() + ")",
                null
        );

        return enrichEmployeeDto(savedEmployee);
    }

    @Transactional
    public EmployeeResponseDto updateEmployee(UserPrincipal user, Long id, EmployeeUpdateRequest request) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can update employee profiles");
        }

        Employee employee = employeeRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        User empUser = userRepository.findById(employee.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        empUser.setFullName(request.getFullName());
        empUser.setPhone(request.getPhone());
        userRepository.save(empUser);

        employee.setDesignation(request.getDesignation());
        employee.setDepartment(request.getDepartment());
        employee.setNotes(request.getNotes());
        Employee updated = employeeRepository.save(employee);

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "UPDATE_EMPLOYEE",
                "EMPLOYEE",
                updated.getId(),
                "Updated profile for employee '" + empUser.getFullName() + "' (" + updated.getEmployeeCode() + ")",
                null
        );

        return enrichEmployeeDto(updated);
    }

    @Transactional
    public void toggleEmployeeStatus(UserPrincipal user, Long id, UserStatus newStatus) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can disable or enable employees");
        }

        Employee employee = employeeRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        User empUser = userRepository.findById(employee.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        empUser.setStatus(newStatus);
        userRepository.save(empUser);

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "UPDATE_EMPLOYEE_STATUS",
                "EMPLOYEE",
                employee.getId(),
                "Changed status of employee '" + empUser.getFullName() + "' (" + employee.getEmployeeCode() + ") to " + newStatus,
                null
        );
    }

    @Transactional
    public void assignClients(UserPrincipal user, Long id, AssignClientsRequest request) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can assign clients to employees");
        }

        Employee employee = employeeRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        List<Long> targetClientIds = (request != null && request.getClientIds() != null)
                ? request.getClientIds() : Collections.emptyList();

        // 1. Delete all assignments (client-level or event-level) for clients that are NOT in targetClientIds
        List<EmployeeClientAccess> existingAssignments = accessRepository.findByEmployeeId(employee.getId());
        List<EmployeeClientAccess> toDelete = existingAssignments.stream()
                .filter(a -> !targetClientIds.contains(a.getClientId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            accessRepository.deleteAll(toDelete);
            accessRepository.flush();
        }

        // 2. Add client-level assignments for target clients with distinct other employee capacity check
        for (Long clientId : targetClientIds) {
            long otherAssignedEmployeesCount = accessRepository.findByClientId(clientId).stream()
                    .map(EmployeeClientAccess::getEmployeeId)
                    .filter(empId -> !empId.equals(employee.getId()))
                    .distinct()
                    .count();
            if (otherAssignedEmployeesCount >= 3) {
                throw new com.turtledot.studio.exception.ValidationException("Client is already assigned to the maximum limit of 3 employees.");
            }

            boolean hasClientLevelAccess = accessRepository.findByEmployeeId(employee.getId()).stream()
                    .anyMatch(a -> a.getClientId().equals(clientId) && a.getEventId() == null);

            if (!hasClientLevelAccess) {
                accessRepository.save(new EmployeeClientAccess(
                        user.getStudioId(), employee.getId(), clientId, null, user.getId()
                ));
            }

            // Sync with all events of this client so assigned crew on event cards is immediately updated
            List<Event> clientEvents = eventRepository.findByStudioIdAndClientId(user.getStudioId(), clientId);
            for (Event ev : clientEvents) {
                boolean hasEventAccess = accessRepository.findByEmployeeId(employee.getId()).stream()
                        .anyMatch(a -> ev.getId().equals(a.getEventId()));
                if (!hasEventAccess) {
                    accessRepository.save(new EmployeeClientAccess(
                            user.getStudioId(), employee.getId(), clientId, ev.getId(), user.getId()
                    ));
                }
            }
        }
        accessRepository.flush();

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "ASSIGN_CLIENTS",
                "EMPLOYEE",
                employee.getId(),
                "Updated assigned clients for employee '" + employee.getEmployeeCode() + "'",
                null
        );
    }

    @Transactional
    public void assignPermissions(UserPrincipal user, Long id, AssignPermissionsRequest request) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can manage employee permissions");
        }

        Employee employee = employeeRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        permissionRepository.deleteAllByEmployeeId(employee.getId());

        if (request.getPermissions() != null) {
            for (String perm : request.getPermissions()) {
                permissionRepository.save(new EmployeePermission(user.getStudioId(), employee.getId(), perm));
            }
        }
    }

    @Transactional
    public void deleteEmployee(UserPrincipal user, Long id) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can delete employees");
        }

        Employee employee = employeeRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (employee.getUserId().equals(user.getId())) {
            throw new ForbiddenException("You cannot delete your own account");
        }

        Long userId = employee.getUserId();

        // 1. Remove access assignments & permissions
        accessRepository.deleteAll(accessRepository.findByEmployeeId(employee.getId()));
        permissionRepository.deleteAllByEmployeeId(employee.getId());

        // 2. Delete employee profile
        employeeRepository.delete(employee);

        // 3. Delete user credentials
        if (userId != null) {
            userRepository.findById(userId).ifPresent(userRepository::delete);
        }
    }

    private EmployeeResponseDto enrichEmployeeDto(Employee employee) {
        User user = userRepository.findById(employee.getUserId()).orElse(null);

        List<String> perms = permissionRepository.findByEmployeeId(employee.getId()).stream()
                .map(EmployeePermission::getPermission)
                .collect(Collectors.toList());

        List<Long> assignedClients = accessRepository.findByEmployeeId(employee.getId()).stream()
                .map(EmployeeClientAccess::getClientId)
                .distinct()
                .filter(clientId -> clientRepository.findById(clientId)
                        .map(c -> c.getStatus() != ClientStatus.DELETED)
                        .orElse(false))
                .collect(Collectors.toList());

        EmployeeResponseDto dto = new EmployeeResponseDto();
        dto.setId(employee.getId());
        dto.setStudioId(employee.getStudioId());
        dto.setUserId(employee.getUserId());
        dto.setEmployeeCode(employee.getEmployeeCode());
        dto.setFullName(user != null ? user.getFullName() : "");
        dto.setEmail(user != null ? user.getEmail() : "");
        dto.setPhone(user != null ? user.getPhone() : "");
        dto.setDesignation(employee.getDesignation());
        dto.setDepartment(employee.getDepartment());
        dto.setNotes(employee.getNotes());
        dto.setStatus(user != null ? user.getStatus() : UserStatus.ACTIVE);
        dto.setPermissions(perms);
        dto.setAssignedClientIds(assignedClients);
        dto.setAssignedClientsCount(assignedClients.size());
        dto.setCreatedAt(employee.getCreatedAt());

        return dto;
    }

    private synchronized String generateEmployeeCode(Long studioId) {
        long count = employeeRepository.countByStudioId(studioId);
        int nextNum = (int) count + 1;
        String candidate = String.format("EMP-%03d", nextNum);

        while (employeeRepository.existsByEmployeeCode(candidate)) {
            nextNum++;
            candidate = String.format("EMP-%03d", nextNum);
        }
        return candidate;
    }
}
