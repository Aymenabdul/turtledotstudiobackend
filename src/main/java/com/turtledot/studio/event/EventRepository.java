package com.turtledot.studio.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByIdAndStudioId(Long id, Long studioId);
    List<Event> findByStudioId(Long studioId);
    List<Event> findByStudioIdAndClientId(Long studioId, Long clientId);
    boolean existsByEventCode(String eventCode);

    @Query("SELECT e FROM Event e WHERE e.studioId = :studioId " +
           "AND (:clientId IS NULL OR e.clientId = :clientId) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:search IS NULL OR LOWER(e.eventName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(e.eventCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Event> searchEvents(
            @Param("studioId") Long studioId,
            @Param("clientId") Long clientId,
            @Param("status") EventStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT e FROM Event e WHERE e.studioId = :studioId AND e.eventDate >= :today ORDER BY e.eventDate ASC")
    List<Event> findUpcomingEvents(@Param("studioId") Long studioId, @Param("today") LocalDate today, Pageable pageable);

    long countByStudioIdAndStatus(Long studioId, EventStatus status);
    long countByStudioId(Long studioId);
}
