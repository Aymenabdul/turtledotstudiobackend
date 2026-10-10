package com.turtledot.studio.studio;

import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.studio.dto.StudioSettingsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "Endpoints for customizing studio profile, branding, and system preferences")
public class StudioSettingsController {

    private final StudioSettingsService settingsService;

    public StudioSettingsController(StudioSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get studio configuration, branding, and currency settings")
    public ResponseEntity<ApiResponse<StudioSettingsDto>> getSettings(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        StudioSettingsDto dto = settingsService.getSettings(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update studio profile, logo, timezone, or currency")
    public ResponseEntity<ApiResponse<StudioSettingsDto>> updateSettings(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody StudioSettingsDto dto
    ) {
        StudioSettingsDto updated = settingsService.updateSettings(userPrincipal, dto);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", updated));
    }
}
