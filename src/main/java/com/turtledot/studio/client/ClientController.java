package com.turtledot.studio.client;

import com.turtledot.studio.client.dto.ClientRequest;
import com.turtledot.studio.client.dto.ClientResponseDto;
import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.security.UserPrincipal;
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
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Endpoints for managing client accounts and directory")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @Operation(summary = "Get paginated clients with optional search and status filters")
    public ResponseEntity<ApiResponse<PageResponse<ClientResponseDto>>> getClients(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) ClientStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        PageResponse<ClientResponseDto> response = clientService.getClients(userPrincipal, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get client details by ID")
    public ResponseEntity<ApiResponse<ClientResponseDto>> getClientById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        ClientResponseDto client = clientService.getClientById(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success(client));
    }

    @PostMapping
    @Operation(summary = "Create a new client (Admin only)")
    public ResponseEntity<ApiResponse<ClientResponseDto>> createClient(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ClientRequest request
    ) {
        ClientResponseDto created = clientService.createClient(userPrincipal, request);
        return ResponseEntity.ok(ApiResponse.success("Client created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update client details")
    public ResponseEntity<ApiResponse<ClientResponseDto>> updateClient(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody ClientRequest request
    ) {
        ClientResponseDto updated = clientService.updateClient(userPrincipal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Client updated successfully", updated));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive a client account")
    public ResponseEntity<ApiResponse<Void>> archiveClient(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        clientService.archiveClient(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success("Client archived successfully", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a client account (requires typed confirmation code: DELETE <clientCode>)")
    public ResponseEntity<ApiResponse<Void>> deleteClient(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestParam String confirmationCode
    ) {
        clientService.deleteClient(userPrincipal, id, confirmationCode);
        return ResponseEntity.ok(ApiResponse.success("Client deleted successfully", null));
    }
}
