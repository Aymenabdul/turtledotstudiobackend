package com.turtledot.studio.search;

import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.search.dto.GlobalSearchResultDto;
import com.turtledot.studio.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Global search endpoint across clients, events, and photo albums")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Global search for clients, events, and albums")
    public ResponseEntity<ApiResponse<GlobalSearchResultDto>> search(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false, defaultValue = "") String q
    ) {
        GlobalSearchResultDto result = searchService.globalSearch(userPrincipal, q);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
