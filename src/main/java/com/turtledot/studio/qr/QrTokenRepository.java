package com.turtledot.studio.qr;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrTokenRepository extends JpaRepository<QrToken, Long> {
    Optional<QrToken> findByTokenValue(String tokenValue);
    Optional<QrToken> findByIdAndStudioId(Long id, Long studioId);
    List<QrToken> findByEventIdAndStudioId(Long eventId, Long studioId);
    Page<QrToken> findByStudioId(Long studioId, Pageable pageable);
}
