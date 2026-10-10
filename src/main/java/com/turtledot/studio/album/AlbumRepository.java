package com.turtledot.studio.album;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    Optional<Album> findByIdAndStudioId(Long id, Long studioId);
    List<Album> findByStudioId(Long studioId);
    List<Album> findByEventIdAndStudioId(Long eventId, Long studioId);
    Page<Album> findByEventIdAndStudioId(Long eventId, Long studioId, Pageable pageable);
    long countByStudioId(Long studioId);
    long countByEventId(Long eventId);
}
