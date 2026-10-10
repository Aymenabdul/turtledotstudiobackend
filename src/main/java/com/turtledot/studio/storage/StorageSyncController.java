package com.turtledot.studio.storage;

import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.storage.dto.StorageSyncReportDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/storage/sync")
@Tag(name = "Storage Synchronization", description = "Endpoints for detecting physical disk and database discrepancies")
public class StorageSyncController {

    private final StorageSyncService storageSyncService;

    public StorageSyncController(StorageSyncService storageSyncService) {
        this.storageSyncService = storageSyncService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Scan physical disk and database media records for missing files or orphans")
    public ResponseEntity<ApiResponse<StorageSyncReportDto>> getSyncReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        StorageSyncReportDto report = storageSyncService.scanStorage(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/prune")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Prune untracked orphan files from physical storage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pruneOrphans(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        int prunedCount = storageSyncService.pruneOrphans(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Pruned " + prunedCount + " orphan files", Map.of("prunedCount", prunedCount)));
    }
}
