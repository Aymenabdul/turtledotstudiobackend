package com.turtledot.studio.event;

import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.event.dto.EventRequest;
import com.turtledot.studio.event.dto.EventResponseDto;
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
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Endpoints for client photography events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Get paginated events with optional client, status, and search filters")
    public ResponseEntity<ApiResponse<PageResponse<EventResponseDto>>> getEvents(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "eventDate"));
        PageResponse<EventResponseDto> response = eventService.getEvents(userPrincipal, clientId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event details by ID")
    public ResponseEntity<ApiResponse<EventResponseDto>> getEventById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        EventResponseDto response = eventService.getEventById(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "Create a new event for a client")
    public ResponseEntity<ApiResponse<EventResponseDto>> createEvent(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody EventRequest request
    ) {
        EventResponseDto created = eventService.createEvent(userPrincipal, request);
        return ResponseEntity.ok(ApiResponse.success("Event created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing event")
    public ResponseEntity<ApiResponse<EventResponseDto>> updateEvent(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request
    ) {
        EventResponseDto updated = eventService.updateEvent(userPrincipal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Event updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an event (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        eventService.deleteEvent(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success("Event deleted successfully", null));
    }
}
