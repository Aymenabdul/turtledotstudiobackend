package com.turtledot.studio.album;

import com.turtledot.studio.album.dto.AlbumRequest;
import com.turtledot.studio.album.dto.AlbumResponseDto;
import com.turtledot.studio.audit.AuditService;
import com.turtledot.studio.event.Event;
import com.turtledot.studio.event.EventRepository;
import com.turtledot.studio.exception.ForbiddenException;
import com.turtledot.studio.exception.ResourceNotFoundException;
import com.turtledot.studio.security.AuthorizationService;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.storage.StorageService;
import com.turtledot.studio.user.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final EventRepository eventRepository;
    private final AuthorizationService authService;
    private final StorageService storageService;
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public AlbumService(AlbumRepository albumRepository,
                        EventRepository eventRepository,
                        AuthorizationService authService,
                        StorageService storageService,
                        JdbcTemplate jdbcTemplate,
                        AuditService auditService) {
        this.albumRepository = albumRepository;
        this.eventRepository = eventRepository;
        this.authService = authService;
        this.storageService = storageService;
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AlbumResponseDto> getAlbumsByEvent(UserPrincipal user, Long eventId) {
        if (user != null) {
            authService.checkEventAccess(user, eventId, "CLIENT_VIEW");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        return albumRepository.findByEventIdAndStudioId(eventId, event.getStudioId()).stream()
                .map(a -> enrichAlbumDto(a, event.getEventName()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlbumResponseDto getAlbumById(UserPrincipal user, Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));

        if (user != null) {
            authService.checkClientAccess(user, album.getClientId(), "CLIENT_VIEW");
        }

        Event event = eventRepository.findById(album.getEventId()).orElse(null);
        return enrichAlbumDto(album, event != null ? event.getEventName() : "");
    }

    @Transactional
    public AlbumResponseDto createAlbum(UserPrincipal user, AlbumRequest request) {
        Event event = eventRepository.findByIdAndStudioId(request.getEventId(), user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + request.getEventId()));

        authService.checkClientAccess(user, event.getClientId(), "ALBUM_CREATE");

        Album album = new Album();
        album.setStudioId(user.getStudioId());
        album.setClientId(event.getClientId());
        album.setEventId(event.getId());
        album.setName(request.getName());
        album.setDescription(request.getDescription());
        album.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getStatus() != null) {
            album.setStatus(request.getStatus());
        }
        album.setIsCommercial(request.getIsCommercial() != null ? request.getIsCommercial() : false);
        album.setPricePerPhoto(request.getPricePerPhoto());
        album.setAllowGuestPurchases(request.getAllowGuestPurchases() != null ? request.getAllowGuestPurchases() : true);

        Album saved = albumRepository.save(album);

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "CREATE_ALBUM",
                "ALBUM",
                saved.getId(),
                "Created album '" + saved.getName() + "' for event '" + event.getEventName() + "'",
                null
        );

        return enrichAlbumDto(saved, event.getEventName());
    }

    @Transactional
    public AlbumResponseDto updateAlbum(UserPrincipal user, Long id, AlbumRequest request) {
        Album album = albumRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));

        authService.checkClientAccess(user, album.getClientId(), "ALBUM_EDIT");

        album.setName(request.getName());
        album.setDescription(request.getDescription());
        album.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getStatus() != null) {
            album.setStatus(request.getStatus());
        }
        if (request.getIsCommercial() != null) {
            album.setIsCommercial(request.getIsCommercial());
        }
        if (request.getPricePerPhoto() != null) {
            album.setPricePerPhoto(request.getPricePerPhoto());
        }
        if (request.getAllowGuestPurchases() != null) {
            album.setAllowGuestPurchases(request.getAllowGuestPurchases());
        }

        Album updated = albumRepository.save(album);
        Event event = eventRepository.findById(updated.getEventId()).orElse(null);

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "UPDATE_ALBUM",
                "ALBUM",
                updated.getId(),
                "Updated album details for '" + updated.getName() + "'",
                null
        );

        return enrichAlbumDto(updated, event != null ? event.getEventName() : "");
    }

    @Transactional
    public void deleteAlbum(UserPrincipal user, Long id) {
        Album album = albumRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));

        authService.checkClientAccess(user, album.getClientId(), "ALBUM_DELETE");

        String albumName = album.getName();
        Long albumId = album.getId();
        Long studioId = album.getStudioId();

        // 1. Physically delete files and directory on disk
        storageService.deleteAlbumDirectory(album.getStudioId(), album.getClientId(), album.getEventId(), album.getId());

        // 2. Delete database media records
        jdbcTemplate.update("DELETE FROM media WHERE album_id = ?", album.getId());

        // 3. Delete album database record
        albumRepository.delete(album);

        auditService.log(
                studioId,
                user.getId(),
                "DELETE_ALBUM",
                "ALBUM",
                albumId,
                "Deleted album '" + albumName + "' and all associated media",
                null
        );
    }

    private AlbumResponseDto enrichAlbumDto(Album album, String eventName) {
        Integer photos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media WHERE album_id = ? AND media_type = 'PHOTO'",
                Integer.class,
                album.getId()
        );

        Integer videos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media WHERE album_id = ? AND media_type = 'VIDEO'",
                Integer.class,
                album.getId()
        );

        return AlbumResponseDto.fromEntity(
                album,
                eventName,
                photos != null ? photos : 0,
                videos != null ? videos : 0
        );
    }
}
