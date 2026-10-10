package com.turtledot.studio.qr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuestPurchaseRepository extends JpaRepository<GuestPurchase, Long> {

    List<GuestPurchase> findByQrToken(String qrToken);

    List<GuestPurchase> findByQrTokenAndGuestPhone(String qrToken, String guestPhone);

    List<GuestPurchase> findByStudioId(Long studioId);

    List<GuestPurchase> findByEventId(Long eventId);

    Optional<GuestPurchase> findByIdAndQrToken(Long id, String qrToken);

    boolean existsByQrTokenAndMediaId(String qrToken, Long mediaId);

    boolean existsByQrTokenAndMediaIdAndGuestPhone(String qrToken, Long mediaId, String guestPhone);
}
