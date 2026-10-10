package com.turtledot.studio.event;

import com.turtledot.studio.audit.AuditService;
import com.turtledot.studio.client.Client;
import com.turtledot.studio.client.ClientRepository;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.employee.Employee;
import com.turtledot.studio.employee.EmployeeClientAccess;
import com.turtledot.studio.employee.EmployeeClientAccessRepository;
import com.turtledot.studio.employee.EmployeeRepository;
import com.turtledot.studio.event.dto.EventRequest;
import com.turtledot.studio.event.dto.EventResponseDto;
import com.turtledot.studio.exception.ForbiddenException;
import com.turtledot.studio.exception.ResourceNotFoundException;
import com.turtledot.studio.exception.ValidationException;
import com.turtledot.studio.notification.EmailService;
import com.turtledot.studio.security.AuthorizationService;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.studio.Studio;
import com.turtledot.studio.studio.StudioRepository;
import com.turtledot.studio.user.Role;
import com.turtledot.studio.user.User;
import com.turtledot.studio.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeClientAccessRepository accessRepository;
    private final UserRepository userRepository;
    private final StudioRepository studioRepository;
    private final AuthorizationService authService;
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;
    private final EmailService emailService;

    public EventService(EventRepository eventRepository,
                        ClientRepository clientRepository,
                        EmployeeRepository employeeRepository,
                        EmployeeClientAccessRepository accessRepository,
                        UserRepository userRepository,
                        StudioRepository studioRepository,
                        AuthorizationService authService,
                        JdbcTemplate jdbcTemplate,
                        AuditService auditService,
                        EmailService emailService) {
        this.eventRepository = eventRepository;
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
        this.accessRepository = accessRepository;
        this.userRepository = userRepository;
        this.studioRepository = studioRepository;
        this.authService = authService;
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public PageResponse<EventResponseDto> getEvents(UserPrincipal user, Long clientId, EventStatus status, String search, Pageable pageable) {
        if (clientId != null) {
            authService.checkClientAccess(user, clientId, "CLIENT_VIEW");
        } else if (user.getRole() == Role.EMPLOYEE) {
            Long employeeId = authService.getEmployeeIdForUser(user.getId());
            if (employeeId == null) {
                throw new ForbiddenException("Employee profile not found");
            }
        }

        Page<Event> page = eventRepository.searchEvents(user.getStudioId(), clientId, status, search, pageable);
        List<EventResponseDto> dtos = page.getContent().stream()
                .filter(e -> {
                    if (user.getRole() == Role.ADMIN) return true;
                    try {
                        authService.checkClientAccess(user, e.getClientId(), "CLIENT_VIEW");
                        return true;
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .map(this::enrichEventDto)
                .collect(Collectors.toList());

        return new PageResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EventResponseDto getEventById(UserPrincipal user, Long id) {
        authService.checkEventAccess(user, id, "CLIENT_VIEW");

        Event event = eventRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        return enrichEventDto(event);
    }

    @Transactional
    public EventResponseDto createEvent(UserPrincipal user, EventRequest request) {
        authService.checkClientAccess(user, request.getClientId(), "CLIENT_EDIT");

        Client client = clientRepository.findByIdAndStudioId(request.getClientId(), user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.getClientId()));

        Event event = new Event();
        event.setStudioId(user.getStudioId());
        event.setClientId(client.getId());
        event.setEventCode(generateEventCode(user.getStudioId()));
        event.setEventName(request.getEventName());
        event.setEventType(request.getEventType());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());
        event.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }

        List<Long> empIdsToAssign = request.getAssignedEmployeeIds();
        if (empIdsToAssign == null || empIdsToAssign.isEmpty()) {
            empIdsToAssign = accessRepository.findByClientId(client.getId()).stream()
                    .map(EmployeeClientAccess::getEmployeeId)
                    .distinct()
                    .collect(Collectors.toList());
            event.setCrewConfigured(false);
        } else {
            empIdsToAssign = empIdsToAssign.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (empIdsToAssign.size() > 3) {
                throw new ValidationException("A maximum of 3 crew members can be assigned to an event/client.");
            }
            event.setCrewConfigured(true);
        }

        Event saved = eventRepository.save(event);

        // Assign employees and synchronize client-level access
        List<String> assignedEmployeeNames = new ArrayList<>();
        if (empIdsToAssign != null && !empIdsToAssign.isEmpty()) {
            for (Long empId : empIdsToAssign) {
                if (empId != null) {
                    Employee employee = employeeRepository.findByIdAndStudioId(empId, user.getStudioId()).orElse(null);
                    if (employee != null) {
                        accessRepository.save(new EmployeeClientAccess(
                                user.getStudioId(), empId, client.getId(), saved.getId(), user.getId()
                        ));

                        // Ensure client-level assignment exists so employee table reflects client assignment
                        boolean hasClientLevel = accessRepository.findByEmployeeId(empId).stream()
                                .anyMatch(a -> client.getId().equals(a.getClientId()) && a.getEventId() == null);
                        if (!hasClientLevel) {
                            accessRepository.save(new EmployeeClientAccess(
                                    user.getStudioId(), empId, client.getId(), null, user.getId()
                            ));
                        }

                        User empUser = userRepository.findById(employee.getUserId()).orElse(null);
                        if (empUser != null) {
                            assignedEmployeeNames.add(empUser.getFullName());
                        }
                    }
                }
            }
            accessRepository.flush();
        }

        Studio studio = studioRepository.findById(user.getStudioId()).orElse(null);

        // 1. Send confirmation email to client
        emailService.sendEventBookingEmailToClient(studio, client, saved, assignedEmployeeNames);

        // 2. Send assignment notification emails to assigned employees
        if (empIdsToAssign != null) {
            for (Long empId : empIdsToAssign) {
                if (empId != null) {
                    Employee employee = employeeRepository.findByIdAndStudioId(empId, user.getStudioId()).orElse(null);
                    if (employee != null) {
                        User empUser = userRepository.findById(employee.getUserId()).orElse(null);
                        if (empUser != null) {
                            emailService.sendEventAssignmentEmailToEmployee(studio, empUser, employee, saved, client);
                        }
                    }
                }
            }
        }

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "CREATE_EVENT",
                "EVENT",
                saved.getId(),
                "Created event gallery '" + saved.getEventName() + "' (" + saved.getEventCode() + ") for client '" + client.getName() + "'",
                null
        );

        return enrichEventDto(saved);
    }

    @Transactional
    public EventResponseDto updateEvent(UserPrincipal user, Long id, EventRequest request) {
        authService.checkEventAccess(user, id, "CLIENT_EDIT");

        Event event = eventRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        if (request.getClientId() != null && !request.getClientId().equals(event.getClientId())) {
            authService.checkClientAccess(user, request.getClientId(), "CLIENT_EDIT");
            Client client = clientRepository.findByIdAndStudioId(request.getClientId(), user.getStudioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.getClientId()));
            event.setClientId(client.getId());
        }

        event.setEventName(request.getEventName());
        event.setEventType(request.getEventType());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());
        event.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }
        if (request.getAssignedEmployeeIds() != null) {
            event.setCrewConfigured(true);
        }

        Event updated = eventRepository.save(event);

        if (request.getAssignedEmployeeIds() != null) {
            List<Long> targetEmpIds = request.getAssignedEmployeeIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (targetEmpIds.size() > 3) {
                throw new ValidationException("A maximum of 3 crew members can be assigned to an event/client.");
            }

            // Find existing assigned employees for this event before deleting
            List<Long> prevAssignedEmpIds = accessRepository.findByEventId(updated.getId()).stream()
                    .map(EmployeeClientAccess::getEmployeeId)
                    .distinct()
                    .collect(Collectors.toList());

            accessRepository.deleteByEventId(updated.getId());
            accessRepository.flush();

            for (Long empId : targetEmpIds) {
                Employee employee = employeeRepository.findByIdAndStudioId(empId, user.getStudioId()).orElse(null);
                if (employee != null) {
                    accessRepository.save(new EmployeeClientAccess(
                            user.getStudioId(), empId, updated.getClientId(), updated.getId(), user.getId()
                    ));

                    // Ensure client-level assignment exists for this employee
                    boolean hasClientLevel = accessRepository.findByEmployeeId(empId).stream()
                            .anyMatch(a -> updated.getClientId().equals(a.getClientId()) && a.getEventId() == null);
                    if (!hasClientLevel) {
                        accessRepository.save(new EmployeeClientAccess(
                                user.getStudioId(), empId, updated.getClientId(), null, user.getId()
                        ));
                    }
                }
            }

            // For any employee previously assigned who is NOT in targetEmpIds:
            // check if they are assigned to other events of this client. If not, remove client-level assignment.
            for (Long prevEmpId : prevAssignedEmpIds) {
                if (!targetEmpIds.contains(prevEmpId)) {
                    boolean hasOtherEventsForClient = accessRepository.findByEmployeeId(prevEmpId).stream()
                            .anyMatch(a -> updated.getClientId().equals(a.getClientId())
                                    && a.getEventId() != null
                                    && !a.getEventId().equals(updated.getId()));
                    if (!hasOtherEventsForClient) {
                        accessRepository.deleteByEmployeeIdAndClientId(prevEmpId, updated.getClientId());
                    }
                }
            }
            accessRepository.flush();
        }

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "UPDATE_EVENT",
                "EVENT",
                updated.getId(),
                "Updated details for event gallery '" + updated.getEventName() + "' (" + updated.getEventCode() + ")",
                null
        );

        return enrichEventDto(updated);
    }

    @Transactional
    public void deleteEvent(UserPrincipal user, Long id) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can delete events");
        }

        Event event = eventRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        String eventName = event.getEventName();
        String eventCode = event.getEventCode();
        Long eventId = event.getId();
        Long studioId = event.getStudioId();

        accessRepository.deleteByEventId(eventId);
        eventRepository.delete(event);

        auditService.log(
                studioId,
                user.getId(),
                "DELETE_EVENT",
                "EVENT",
                eventId,
                "Deleted event gallery '" + eventName + "' (" + eventCode + ")",
                null
        );
    }

    private EventResponseDto enrichEventDto(Event event) {
        Client client = clientRepository.findById(event.getClientId()).orElse(null);

        Integer albumCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM albums WHERE event_id = ? AND studio_id = ?",
                Integer.class,
                event.getId(), event.getStudioId()
        );

        Integer mediaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media WHERE event_id = ? AND studio_id = ?",
                Integer.class,
                event.getId(), event.getStudioId()
        );

        String qrToken = null;
        try {
            qrToken = jdbcTemplate.queryForObject(
                    "SELECT token_value FROM qr_tokens WHERE event_id = ? AND status = 'ACTIVE' LIMIT 1",
                    String.class,
                    event.getId()
            );
        } catch (Exception ignored) {}

        List<Long> assignedEmployeeIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT emp.id FROM employees emp " +
                "JOIN employee_client_access eca ON eca.employee_id = emp.id " +
                "WHERE (eca.event_id = ? OR (eca.client_id = ? AND eca.event_id IS NULL))",
                Long.class,
                event.getId(),
                event.getClientId()
        );

        List<String> assignedEmployeeNames = jdbcTemplate.queryForList(
                "SELECT DISTINCT u.full_name FROM employees emp " +
                "JOIN users u ON emp.user_id = u.id " +
                "JOIN employee_client_access eca ON eca.employee_id = emp.id " +
                "WHERE (eca.event_id = ? OR (eca.client_id = ? AND eca.event_id IS NULL))",
                String.class,
                event.getId(),
                event.getClientId()
        );

        EventResponseDto dto = EventResponseDto.fromEntity(
                event,
                client != null ? client.getName() : "Unknown Client",
                client != null ? client.getClientCode() : "",
                albumCount != null ? albumCount : 0,
                mediaCount != null ? mediaCount : 0,
                qrToken
        );
        dto.setAssignedEmployeeIds(assignedEmployeeIds);
        dto.setAssignedEmployeeNames(assignedEmployeeNames);
        return dto;
    }

    private synchronized String generateEventCode(Long studioId) {
        int currentYear = Year.now().getValue();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE studio_id = ?",
                Integer.class,
                studioId
        );
        int nextNum = (count != null ? count : 0) + 1;
        String candidate = String.format("EV-%d-%04d", currentYear, nextNum);

        while (eventRepository.existsByEventCode(candidate)) {
            nextNum++;
            candidate = String.format("EV-%d-%04d", currentYear, nextNum);
        }
        return candidate;
    }
}
