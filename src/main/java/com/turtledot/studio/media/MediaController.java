package com.turtledot.studio.media;

import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.media.dto.MediaResponseDto;
import com.turtledot.studio.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Media", description = "Endpoints for uploading, streaming, and managing photos and videos")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a photo or video to an album")
    public ResponseEntity<ApiResponse<MediaResponseDto>> uploadMedia(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("albumId") Long albumId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        MediaResponseDto dto = mediaService.uploadMedia(userPrincipal, albumId, file);
        return ResponseEntity.ok(ApiResponse.success("Media uploaded successfully", dto));
    }

    @GetMapping("/album/{albumId}")
    @Operation(summary = "Get all media items in an album")
    public ResponseEntity<ApiResponse<List<MediaResponseDto>>> getAlbumMedia(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long albumId
    ) {
        List<MediaResponseDto> list = mediaService.getAlbumMedia(userPrincipal, albumId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Stream or download original photo or video file")
    public ResponseEntity<?> streamMedia(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestParam(value = "download", defaultValue = "false") boolean download
    ) throws IOException {
        if (download) {
            mediaService.checkDownloadAccess(userPrincipal, id);
        } else {
            mediaService.checkViewAccess(userPrincipal, id);
        }

        Media media = mediaService.getMediaEntity(id);
        if (media.getFileUrl() != null && media.getFileUrl().startsWith("http")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(media.getFileUrl())).build();
        }

        Resource resource = download 
                ? mediaService.loadMediaFileForDownload(userPrincipal, id) 
                : mediaService.loadMediaFile(userPrincipal, id);
        String contentType = resolveContentType(media);
        String disposition = download ? "attachment" : "inline";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + media.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download photo or video file directly as attachment")
    public ResponseEntity<?> downloadMedia(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) throws IOException {
        mediaService.checkDownloadAccess(userPrincipal, id);

        Media media = mediaService.getMediaEntity(id);
        if (media.getFileUrl() != null && media.getFileUrl().startsWith("http")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(media.getFileUrl())).build();
        }

        Resource resource = mediaService.loadMediaFileForDownload(userPrincipal, id);
        String contentType = resolveContentType(media);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + media.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/stream")
    @Operation(summary = "Stream video with HTTP byte range support")
    public ResponseEntity<ResourceRegion> streamVideo(
            @PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            @RequestHeader HttpHeaders headers
    ) throws IOException {
        Media media = mediaService.getMediaEntity(id);
        Resource resource = mediaService.loadMediaFile(null, id);

        String contentType = resolveContentType(media);
        MediaType mediaType = MediaType.parseMediaType(contentType);

        long contentLength = resource.contentLength();
        List<HttpRange> ranges = headers.getRange();

        if (ranges != null && !ranges.isEmpty()) {
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(1024 * 1024 * 5, end - start + 1); // 5MB chunk
            ResourceRegion region = new ResourceRegion(resource, start, rangeLength);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(mediaType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(region);
        } else {
            ResourceRegion region = new ResourceRegion(resource, 0, contentLength);
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(mediaType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(region);
        }
    }

    private String resolveContentType(Media media) {
        String contentType = media.getMimeType();
        if (contentType == null || contentType.isBlank() || contentType.equals("application/octet-stream")) {
            String filename = media.getFileName() != null ? media.getFileName().toLowerCase() : "";
            if (filename.endsWith(".mp4") || filename.endsWith(".m4v")) return "video/mp4";
            if (filename.endsWith(".webm")) return "video/webm";
            if (filename.endsWith(".mov")) return "video/quicktime";
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
            if (filename.endsWith(".png")) return "image/png";
            if (filename.endsWith(".webp")) return "image/webp";
            return "application/octet-stream";
        }
        return contentType;
    }

    @GetMapping("/{id}/thumbnail")
    @Operation(summary = "Get media thumbnail (300x300)")
    public ResponseEntity<?> getThumbnail(@PathVariable Long id) throws IOException {
        Media media = mediaService.getMediaEntity(id);
        if (media.getThumbnailUrl() != null && media.getThumbnailUrl().startsWith("http")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(media.getThumbnailUrl())).build();
        }
        if (media.getFileUrl() != null && media.getFileUrl().startsWith("http")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(media.getFileUrl())).build();
        }
        Resource resource = mediaService.loadThumbnail(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Get web preview image (1200x800)")
    public ResponseEntity<?> getPreview(@PathVariable Long id) throws IOException {
        Media media = mediaService.getMediaEntity(id);
        if (media.getPreviewUrl() != null && media.getPreviewUrl().startsWith("http")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(media.getPreviewUrl())).build();
        }
        if (media.getFileUrl() != null && media.getFileUrl().startsWith("http")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(media.getFileUrl())).build();
        }
        Resource resource = mediaService.loadPreview(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @PostMapping("/{id}/favorite")
    @Operation(summary = "Toggle favorite state of a photo")
    public ResponseEntity<ApiResponse<Void>> toggleFavorite(@PathVariable Long id) {
        mediaService.toggleFavorite(id);
        return ResponseEntity.ok(ApiResponse.success("Favorite updated", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a media item")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        mediaService.deleteMedia(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success("Media deleted successfully", null));
    }
}
