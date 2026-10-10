package com.turtledot.studio.client;

import com.turtledot.studio.audit.AuditService;
import com.turtledot.studio.client.dto.ClientRequest;
import com.turtledot.studio.client.dto.ClientResponseDto;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.exception.ForbiddenException;
import com.turtledot.studio.exception.ResourceNotFoundException;
import com.turtledot.studio.exception.ValidationException;
import com.turtledot.studio.security.AuthorizationService;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final AuthorizationService authService;
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public ClientService(ClientRepository clientRepository,
                         AuthorizationService authService,
                         JdbcTemplate jdbcTemplate,
                         AuditService auditService) {
        this.clientRepository = clientRepository;
        this.authService = authService;
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientResponseDto> getClients(UserPrincipal user, ClientStatus status, String search, Pageable pageable) {
        Page<Client> clientPage;

        if (user.getRole() == Role.ADMIN) {
            clientPage = clientRepository.searchClients(user.getStudioId(), status, search, pageable);
        } else if (user.getRole() == Role.EMPLOYEE) {
            Long employeeId = authService.getEmployeeIdForUser(user.getId());
            if (employeeId == null) {
                throw new ForbiddenException("Employee profile not found");
            }
            clientPage = clientRepository.findAssignedClientsForEmployee(user.getStudioId(), employeeId, search, pageable);
        } else {
            throw new ForbiddenException("Clients cannot view client listings");
        }

        List<ClientResponseDto> dtos = clientPage.getContent().stream()
                .map(this::enrichClientDto)
                .collect(Collectors.toList());

        return new PageResponse<>(dtos, clientPage.getNumber(), clientPage.getSize(), clientPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ClientResponseDto getClientById(UserPrincipal user, Long id) {
        authService.checkClientAccess(user, id, "CLIENT_VIEW");

        Client client = clientRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

        return enrichClientDto(client);
    }

    @Transactional
    public ClientResponseDto createClient(UserPrincipal user, ClientRequest request) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can create clients");
        }

        Client client = new Client();
        client.setStudioId(user.getStudioId());
        client.setClientCode(generateClientCode(user.getStudioId()));
        applyRequestData(client, request);

        Client saved = clientRepository.save(client);

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "CREATE_CLIENT",
                "CLIENT",
                saved.getId(),
                "Created client profile '" + saved.getName() + "' (" + saved.getClientCode() + ")",
                null
        );

        return enrichClientDto(saved);
    }

    @Transactional
    public ClientResponseDto updateClient(UserPrincipal user, Long id, ClientRequest request) {
        authService.checkClientAccess(user, id, "CLIENT_EDIT");

        Client client = clientRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

        applyRequestData(client, request);
        Client updated = clientRepository.save(client);

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "UPDATE_CLIENT",
                "CLIENT",
                updated.getId(),
                "Updated client profile for '" + updated.getName() + "' (" + updated.getClientCode() + ")",
                null
        );

        return enrichClientDto(updated);
    }

    @Transactional
    public void archiveClient(UserPrincipal user, Long id) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can archive clients");
        }

        Client client = clientRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

        client.setStatus(ClientStatus.ARCHIVED);
        clientRepository.save(client);

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "ARCHIVE_CLIENT",
                "CLIENT",
                client.getId(),
                "Archived client profile '" + client.getName() + "' (" + client.getClientCode() + ")",
                null
        );
    }

    @Transactional
    public void deleteClient(UserPrincipal user, Long id, String confirmationCode) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can permanently delete clients");
        }

        Client client = clientRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

        String requiredConfirmation = "DELETE " + client.getClientCode();
        if (!requiredConfirmation.equals(confirmationCode)) {
            throw new ValidationException("Confirmation code mismatch. Please type: " + requiredConfirmation);
        }

        String clientName = client.getName();
        String clientCode = client.getClientCode();
        Long clientId = client.getId();
        Long studioId = client.getStudioId();

        client.setStatus(ClientStatus.DELETED);
        clientRepository.save(client);

        // Clean up any employee assignments for this deleted client
        jdbcTemplate.update("DELETE FROM employee_client_access WHERE client_id = ?", id);

        auditService.log(
                studioId,
                user.getId(),
                "DELETE_CLIENT",
                "CLIENT",
                clientId,
                "Permanently deleted client profile '" + clientName + "' (" + clientCode + ")",
                null
        );
    }

    private void applyRequestData(Client client, ClientRequest req) {
        client.setName(req.getName());
        client.setEmail(req.getEmail());
        client.setPhone(req.getPhone());
        client.setAlternatePhone(req.getAlternatePhone());
        client.setAddress(req.getAddress());
        client.setCity(req.getCity());
        client.setState(req.getState());
        client.setCountry(req.getCountry());
        client.setProfileImageUrl(req.getProfileImageUrl());
        if (req.getStatus() != null) {
            client.setStatus(req.getStatus());
        }
        client.setNotes(req.getNotes());
        if (req.getTotalAmount() != null) {
            client.setTotalAmount(req.getTotalAmount());
        }
    }

    private ClientResponseDto enrichClientDto(Client client) {
        BigDecimal paid = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0.00) FROM payments WHERE client_id = ? AND studio_id = ?",
                BigDecimal.class,
                client.getId(), client.getStudioId()
        );

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE client_id = ? AND studio_id = ?",
                Integer.class,
                client.getId(), client.getStudioId()
        );

        return ClientResponseDto.fromEntity(client, paid, eventCount != null ? eventCount : 0);
    }

    private synchronized String generateClientCode(Long studioId) {
        int currentYear = Year.now().getValue();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clients WHERE studio_id = ?",
                Integer.class,
                studioId
        );
        int nextNum = (count != null ? count : 0) + 1;
        String candidate = String.format("CL-%d-%06d", currentYear, nextNum);

        while (clientRepository.existsByClientCode(candidate)) {
            nextNum++;
            candidate = String.format("CL-%d-%06d", currentYear, nextNum);
        }
        return candidate;
    }
}
