package com.turtledot.studio.album;

import com.turtledot.studio.album.dto.AlbumRequest;
import com.turtledot.studio.album.dto.AlbumResponseDto;
import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/albums")
@Tag(name = "Albums", description = "Endpoints for client photo and video albums")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @GetMapping
    @Operation(summary = "Get all albums for a given event")
    public ResponseEntity<ApiResponse<List<AlbumResponseDto>>> getAlbums(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Long eventId
    ) {
        List<AlbumResponseDto> response = albumService.getAlbumsByEvent(userPrincipal, eventId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get album details by ID")
    public ResponseEntity<ApiResponse<AlbumResponseDto>> getAlbumById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        AlbumResponseDto response = albumService.getAlbumById(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "Create a new album for an event")
    public ResponseEntity<ApiResponse<AlbumResponseDto>> createAlbum(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody AlbumRequest request
    ) {
        AlbumResponseDto created = albumService.createAlbum(userPrincipal, request);
        return ResponseEntity.ok(ApiResponse.success("Album created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update album name, description, or cover image")
    public ResponseEntity<ApiResponse<AlbumResponseDto>> updateAlbum(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody AlbumRequest request
    ) {
        AlbumResponseDto updated = albumService.updateAlbum(userPrincipal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Album updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an album and its physical media assets")
    public ResponseEntity<ApiResponse<Void>> deleteAlbum(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        albumService.deleteAlbum(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success("Album deleted successfully", null));
    }
}
