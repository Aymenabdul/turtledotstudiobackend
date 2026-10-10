package com.turtledot.studio.media;

import com.turtledot.studio.album.Album;
import com.turtledot.studio.album.AlbumRepository;
import com.turtledot.studio.event.Event;
import com.turtledot.studio.event.EventRepository;
import com.turtledot.studio.exception.ForbiddenException;
import com.turtledot.studio.exception.ResourceNotFoundException;
import com.turtledot.studio.exception.ValidationException;
import com.turtledot.studio.media.dto.MediaResponseDto;
import com.turtledot.studio.security.AuthorizationService;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.storage.ImageProcessingService;
import com.turtledot.studio.storage.StorageService;
import com.turtledot.studio.storage.VideoProcessingService;
import com.turtledot.studio.client.Client;
import com.turtledot.studio.client.ClientRepository;
import com.turtledot.studio.payment.PaymentRepository;
import com.turtledot.studio.user.Role;
import com.turtledot.studio.audit.AuditService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    private final MediaRepository mediaRepository;
    private final AlbumRepository albumRepository;
    private final EventRepository eventRepository;
    private final StorageService storageService;
    private final ImageProcessingService imageProcessingService;
    private final VideoProcessingService videoProcessingService;
    private final AuthorizationService authService;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    private static final Set<String> ALLOWED_PHOTO_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_VIDEO_EXT = Set.of("mp4", "mov", "m4v", "webm");

    public MediaService(MediaRepository mediaRepository,
                        AlbumRepository albumRepository,
                        EventRepository eventRepository,
                        StorageService storageService,
                        ImageProcessingService imageProcessingService,
                        VideoProcessingService videoProcessingService,
                        AuthorizationService authService,
                        ClientRepository clientRepository,
                        PaymentRepository paymentRepository,
                        AuditService auditService) {
        this.mediaRepository = mediaRepository;
        this.albumRepository = albumRepository;
        this.eventRepository = eventRepository;
        this.storageService = storageService;
        this.imageProcessingService = imageProcessingService;
        this.videoProcessingService = videoProcessingService;
        this.authService = authService;
        this.clientRepository = clientRepository;
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public MediaResponseDto uploadMedia(UserPrincipal user, Long albumId, MultipartFile file) throws IOException {
        Album album = albumRepository.findByIdAndStudioId(albumId, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + albumId));

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ValidationException("Filename cannot be empty");
        }

        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        boolean isPhoto = ALLOWED_PHOTO_EXT.contains(extension);
        boolean isVideo = ALLOWED_VIDEO_EXT.contains(extension);

        if (!isPhoto && !isVideo) {
            throw new ValidationException("Unsupported file type: ." + extension);
        }

        String requiredPerm = isPhoto ? "PHOTO_UPLOAD" : "VIDEO_UPLOAD";
        authService.checkClientAccess(user, album.getClientId(), requiredPerm);

        String storedFileName = UUID.randomUUID() + "." + extension;
        MediaType mediaType = isPhoto ? MediaType.PHOTO : MediaType.VIDEO;

        // 1. Store original file
        String physicalFilePath = storageService.storeFile(
                album.getStudioId(), album.getClientId(), album.getEventId(), album.getId(),
                mediaType.name(), storedFileName, file
        );

        File storedFile = new File(physicalFilePath);

        // 2. Extract dimensions and generate thumbnails
        int width = 1920;
        int height = 1080;
        long duration = 0;
        String baseName = storedFileName.contains(".")
                ? storedFileName.substring(0, storedFileName.lastIndexOf('.'))
                : storedFileName;
        String thumbnailStoredName = baseName + "_thumb.jpg";
        String previewStoredName = baseName + "_preview.jpg";

        if (isPhoto) {
            ImageProcessingService.ImageDimensions dims = imageProcessingService.extractDimensions(storedFile);
            width = dims.width;
            height = dims.height;

            byte[] thumbBytes = imageProcessingService.generateThumbnail(storedFile, 300, 300);
            storageService.storeStream(
                    album.getStudioId(), album.getClientId(), album.getEventId(), album.getId(),
                    "thumbnails", thumbnailStoredName, new ByteArrayInputStream(thumbBytes)
            );

            byte[] previewBytes = imageProcessingService.generateWebPreview(storedFile, 1200, 800);
            storageService.storeStream(
                    album.getStudioId(), album.getClientId(), album.getEventId(), album.getId(),
                    "thumbnails", previewStoredName, new ByteArrayInputStream(previewBytes)
            );
        } else {
            byte[] thumbBytes = videoProcessingService.generateVideoThumbnail(storedFile);
            storageService.storeStream(
                    album.getStudioId(), album.getClientId(), album.getEventId(), album.getId(),
                    "thumbnails", thumbnailStoredName, new ByteArrayInputStream(thumbBytes)
            );
            previewStoredName = thumbnailStoredName;
        }

        // 3. Save Media record
        Media media = new Media();
        media.setStudioId(album.getStudioId());
        media.setClientId(album.getClientId());
        media.setEventId(album.getEventId());
        media.setAlbumId(album.getId());
        media.setMediaType(mediaType);
        media.setFileName(originalFilename);
        media.setStoredFileName(storedFileName);
        media.setFilePath(physicalFilePath);
        String detectedMime = file.getContentType();
        if (detectedMime == null || detectedMime.isBlank() || detectedMime.equals("application/octet-stream")) {
            String lower = originalFilename.toLowerCase();
            if (lower.endsWith(".mp4") || lower.endsWith(".m4v")) detectedMime = "video/mp4";
            else if (lower.endsWith(".webm")) detectedMime = "video/webm";
            else if (lower.endsWith(".mov")) detectedMime = "video/quicktime";
            else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) detectedMime = "image/jpeg";
            else if (lower.endsWith(".png")) detectedMime = "image/png";
            else if (lower.endsWith(".webp")) detectedMime = "image/webp";
            else detectedMime = isPhoto ? "image/jpeg" : "video/mp4";
        }
        media.setMimeType(detectedMime);
        media.setFileSize(file.getSize());
        media.setWidth(width);
        media.setHeight(height);
        media.setDuration(duration);
        media.setUploadedBy(user.getId());

        Media saved = mediaRepository.save(media);

        // Update URLs
        saved.setFileUrl("/api/media/" + saved.getId());
        saved.setThumbnailUrl("/api/media/" + saved.getId() + "/thumbnail");
        saved.setPreviewUrl("/api/media/" + saved.getId() + "/preview");
        Media fullySaved = mediaRepository.save(saved);

        // Set cover image for album if missing
        if (album.getCoverImageUrl() == null && isPhoto) {
            album.setCoverImageUrl(fullySaved.getPreviewUrl());
            albumRepository.save(album);
        }

        auditService.log(
                album.getStudioId(),
                user != null ? user.getId() : null,
                "UPLOAD_MEDIA",
                "MEDIA",
                fullySaved.getId(),
                "Uploaded " + (isPhoto ? "photo" : "video") + " '" + originalFilename + "' to album '" + album.getName() + "'",
                null
        );

        return MediaResponseDto.fromEntity(fullySaved);
    }

    @Transactional(readOnly = true)
    public List<MediaResponseDto> getAlbumMedia(UserPrincipal user, Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + albumId));

        if (user != null) {
            authService.checkClientAccess(user, album.getClientId(), "CLIENT_VIEW");
        }

        return mediaRepository.findByAlbumIdAndStudioId(albumId, album.getStudioId()).stream()
                .map(MediaResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource loadMediaFile(UserPrincipal user, Long mediaId) throws IOException {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        if (user != null) {
            authService.checkClientAccess(user, media.getClientId(), "CLIENT_VIEW");
        }

        enforcePaymentAccess(user, media);

        return storageService.loadAsResource(media.getFilePath());
    }

    public void checkDownloadAccess(UserPrincipal user, Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));
        if (user != null) {
            authService.checkClientAccess(user, media.getClientId(), "MEDIA_DOWNLOAD");
        }
        enforceDownloadAccess(user, media);
    }

    public void checkViewAccess(UserPrincipal user, Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));
        if (user != null) {
            authService.checkClientAccess(user, media.getClientId(), "MEDIA_DOWNLOAD");
        }
        enforcePaymentAccess(user, media);
    }

    public Resource loadMediaFileForDownload(UserPrincipal user, Long mediaId) throws IOException {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        if (user != null) {
            authService.checkClientAccess(user, media.getClientId(), "MEDIA_DOWNLOAD");
        }

        enforceDownloadAccess(user, media);

        return storageService.loadAsResource(media.getFilePath());
    }

    public void enforceDownloadAccess(UserPrincipal user, Media media) {
        if (user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.EMPLOYEE)) {
            return;
        }

        Client client = clientRepository.findById(media.getClientId()).orElse(null);
        if (client == null || client.getTotalAmount() == null || client.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal paid = paymentRepository.sumAmountByClientIdAndStudioId(client.getId(), client.getStudioId());
        if (paid == null) paid = BigDecimal.ZERO;
        BigDecimal target100 = client.getTotalAmount().setScale(2, RoundingMode.HALF_UP);

        if (paid.compareTo(target100) < 0) {
            throw new ForbiddenException("Please complete 100% of the payment to download photos, videos, or albums.");
        }
    }

    private void enforcePaymentAccess(UserPrincipal user, Media media) {
        if (user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.EMPLOYEE)) {
            return;
        }

        Client client = clientRepository.findById(media.getClientId()).orElse(null);
        if (client == null || client.getTotalAmount() == null || client.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal paid = paymentRepository.sumAmountByClientIdAndStudioId(client.getId(), client.getStudioId());
        if (paid == null) paid = BigDecimal.ZERO;
        BigDecimal target80 = client.getTotalAmount().multiply(BigDecimal.valueOf(0.80)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal target50 = client.getTotalAmount().multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);

        if (media.getMediaType() == MediaType.VIDEO && paid.compareTo(target80) < 0) {
            throw new ForbiddenException("Please complete 80% of the payment to get full access to videos.");
        }

        if (media.getMediaType() == MediaType.PHOTO && paid.compareTo(target50) < 0) {
            throw new ForbiddenException("Please complete 50% of the payment to access event photos.");
        }
    }

    @Transactional(readOnly = true)
    public Resource loadThumbnail(Long mediaId) throws IOException {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        if (media.getFilePath() != null) {
            File mainFile = new File(media.getFilePath());
            if (mainFile.exists()) {
                File parent = mainFile.getParentFile();
                if (parent != null && parent.getParentFile() != null) {
                    File thumbDir = new File(parent.getParentFile(), "thumbnails");
                    String storedName = media.getStoredFileName();
                    String base = storedName != null && storedName.contains(".")
                            ? storedName.substring(0, storedName.lastIndexOf('.'))
                            : (storedName != null ? storedName : String.valueOf(media.getId()));

                    File specificThumb = new File(thumbDir, base + "_thumb.jpg");
                    if (specificThumb.exists()) {
                        return storageService.loadAsResource(specificThumb.getAbsolutePath());
                    }

                    if (media.getMediaType() == MediaType.PHOTO) {
                        try {
                            byte[] thumbBytes = imageProcessingService.generateThumbnail(mainFile, 300, 300);
                            Files.createDirectories(thumbDir.toPath());
                            Files.write(specificThumb.toPath(), thumbBytes);
                            return storageService.loadAsResource(specificThumb.getAbsolutePath());
                        } catch (Exception e) {
                            log.warn("Could not generate thumbnail on the fly for media {}: {}", mediaId, e.getMessage());
                            return storageService.loadAsResource(media.getFilePath());
                        }
                    } else if (media.getMediaType() == MediaType.VIDEO) {
                        try {
                            byte[] thumbBytes = videoProcessingService.generateVideoThumbnail(mainFile);
                            Files.createDirectories(thumbDir.toPath());
                            Files.write(specificThumb.toPath(), thumbBytes);
                            return storageService.loadAsResource(specificThumb.getAbsolutePath());
                        } catch (Exception e) {
                            log.warn("Could not generate video thumbnail on the fly for media {}: {}", mediaId, e.getMessage());
                        }
                    }
                }
                return storageService.loadAsResource(media.getFilePath());
            }
        }
        throw new ResourceNotFoundException("Thumbnail not found for media id: " + mediaId);
    }

    @Transactional(readOnly = true)
    public Resource loadPreview(Long mediaId) throws IOException {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        if (media.getFilePath() != null) {
            File mainFile = new File(media.getFilePath());
            if (mainFile.exists()) {
                File parent = mainFile.getParentFile();
                if (parent != null && parent.getParentFile() != null) {
                    File thumbDir = new File(parent.getParentFile(), "thumbnails");
                    String storedName = media.getStoredFileName();
                    String base = storedName != null && storedName.contains(".")
                            ? storedName.substring(0, storedName.lastIndexOf('.'))
                            : (storedName != null ? storedName : String.valueOf(media.getId()));

                    File specificPreview = new File(thumbDir, base + "_preview.jpg");
                    if (specificPreview.exists()) {
                        return storageService.loadAsResource(specificPreview.getAbsolutePath());
                    }

                    if (media.getMediaType() == MediaType.PHOTO) {
                        try {
                            byte[] previewBytes = imageProcessingService.generateWebPreview(mainFile, 1200, 800);
                            Files.createDirectories(thumbDir.toPath());
                            Files.write(specificPreview.toPath(), previewBytes);
                            return storageService.loadAsResource(specificPreview.getAbsolutePath());
                        } catch (Exception e) {
                            log.warn("Could not generate preview on the fly for media {}: {}", mediaId, e.getMessage());
                            return storageService.loadAsResource(media.getFilePath());
                        }
                    } else if (media.getMediaType() == MediaType.VIDEO) {
                        File specificThumb = new File(thumbDir, base + "_thumb.jpg");
                        if (specificThumb.exists()) {
                            return storageService.loadAsResource(specificThumb.getAbsolutePath());
                        }
                    }
                }
                return storageService.loadAsResource(media.getFilePath());
            }
        }
        throw new ResourceNotFoundException("Preview not found for media id: " + mediaId);
    }

    @Transactional
    public void toggleFavorite(Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));
        media.setIsFavorite(!media.getIsFavorite());
        mediaRepository.save(media);
    }

    @Transactional
    public void deleteMedia(UserPrincipal user, Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        String perm = media.getMediaType() == MediaType.PHOTO ? "PHOTO_DELETE" : "VIDEO_DELETE";
        authService.checkClientAccess(user, media.getClientId(), perm);

        String albumName = albumRepository.findById(media.getAlbumId())
                .map(Album::getName)
                .orElse("album #" + media.getAlbumId());
        String fileName = media.getFileName();
        String mediaTypeStr = media.getMediaType() == MediaType.PHOTO ? "photo" : "video";
        Long studioId = media.getStudioId();
        Long entityId = media.getId();

        // Delete physical file
        storageService.deleteFile(media.getFilePath());

        // Delete DB record
        mediaRepository.delete(media);

        auditService.log(
                studioId,
                user != null ? user.getId() : null,
                "DELETE_MEDIA",
                "MEDIA",
                entityId,
                "Deleted " + mediaTypeStr + " '" + fileName + "' from album '" + albumName + "'",
                null
        );
    }

    public Media getMediaEntity(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));
    }
}
