package com.turtledot.studio.media;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    Optional<Media> findByIdAndStudioId(Long id, Long studioId);
    Page<Media> findByAlbumIdAndStudioId(Long albumId, Long studioId, Pageable pageable);
    List<Media> findByAlbumIdAndStudioId(Long albumId, Long studioId);
    List<Media> findByEventIdAndStudioId(Long eventId, Long studioId);
    List<Media> findByClientIdAndStudioId(Long clientId, Long studioId);

    long countByStudioId(Long studioId);
    long countByStudioIdAndMediaType(Long studioId, MediaType mediaType);

    @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM Media m WHERE m.studioId = :studioId")
    long getTotalStorageUsedBytes(@Param("studioId") Long studioId);

    @Query("SELECT m FROM Media m WHERE m.albumId = :albumId AND (:favoriteOnly = false OR m.isFavorite = true)")
    List<Media> findGalleryMedia(@Param("albumId") Long albumId, @Param("favoriteOnly") boolean favoriteOnly);
}
