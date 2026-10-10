package com.turtledot.studio.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByStudioId(Long studioId, Pageable pageable);

    List<Payment> findByClientIdAndStudioIdOrderByPaymentDateDesc(Long clientId, Long studioId);

    List<Payment> findByEventIdAndStudioIdOrderByPaymentDateDesc(Long eventId, Long studioId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.studioId = :studioId")
    BigDecimal sumTotalRevenueByStudioId(@Param("studioId") Long studioId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.clientId = :clientId AND p.studioId = :studioId")
    BigDecimal sumAmountByClientIdAndStudioId(@Param("clientId") Long clientId, @Param("studioId") Long studioId);

    @Query("SELECT p FROM Payment p WHERE p.studioId = :studioId AND p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate ASC")
    List<Payment> findByStudioIdAndDateRange(
            @Param("studioId") Long studioId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
