package com.turtledot.studio.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.turtledot.studio.album.Album;
import com.turtledot.studio.album.AlbumRepository;
import com.turtledot.studio.client.Client;
import com.turtledot.studio.client.ClientRepository;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.event.Event;
import com.turtledot.studio.event.EventRepository;
import com.turtledot.studio.exception.ForbiddenException;
import com.turtledot.studio.exception.ResourceNotFoundException;
import com.turtledot.studio.media.Media;
import com.turtledot.studio.media.MediaRepository;
import com.turtledot.studio.media.MediaType;
import com.turtledot.studio.media.dto.MediaResponseDto;
import com.turtledot.studio.qr.dto.ClientGalleryDto;
import com.turtledot.studio.qr.dto.QrResponseDto;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.studio.Studio;
import com.turtledot.studio.studio.StudioRepository;
import com.turtledot.studio.user.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.turtledot.studio.payment.Payment;
import com.turtledot.studio.payment.PaymentRepository;
import com.turtledot.studio.qr.dto.GalleryPaymentRequest;
import com.turtledot.studio.exception.ValidationException;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class QrService {

    private final QrTokenRepository qrTokenRepository;
    private final EventRepository eventRepository;
    private final ClientRepository clientRepository;
    private final StudioRepository studioRepository;
    private final AlbumRepository albumRepository;
    private final MediaRepository mediaRepository;
    private final PaymentRepository paymentRepository;
    private final com.turtledot.studio.payment.InvoicePdfService invoicePdfService;
    private final GuestPurchaseRepository guestPurchaseRepository;
    private final com.turtledot.studio.audit.AuditService auditService;

    @Value("${app.gallery.base-url:http://localhost:3000/gallery/}")
    private String galleryBaseUrl;

    private static final String TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public QrService(QrTokenRepository qrTokenRepository,
                     EventRepository eventRepository,
                     ClientRepository clientRepository,
                     StudioRepository studioRepository,
                     AlbumRepository albumRepository,
                     MediaRepository mediaRepository,
                     PaymentRepository paymentRepository,
                     com.turtledot.studio.payment.InvoicePdfService invoicePdfService,
                     GuestPurchaseRepository guestPurchaseRepository,
                     com.turtledot.studio.audit.AuditService auditService) {
        this.qrTokenRepository = qrTokenRepository;
        this.eventRepository = eventRepository;
        this.clientRepository = clientRepository;
        this.studioRepository = studioRepository;
        this.albumRepository = albumRepository;
        this.mediaRepository = mediaRepository;
        this.paymentRepository = paymentRepository;
        this.invoicePdfService = invoicePdfService;
        this.guestPurchaseRepository = guestPurchaseRepository;
        this.auditService = auditService;
    }

    @Transactional
    public QrResponseDto generateQrForEvent(UserPrincipal user, Long eventId) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can generate QR tokens");
        }

        Event event = eventRepository.findByIdAndStudioId(eventId, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        // Check if active QR token already exists
        List<QrToken> existing = qrTokenRepository.findByEventIdAndStudioId(eventId, user.getStudioId());
        for (QrToken t : existing) {
            if (t.getStatus() == QrTokenStatus.ACTIVE && !t.isExpired()) {
                return enrichQrDto(t, event);
            }
        }

        String token = generateSecureToken();
        Instant expiresAt = Instant.now().plus(365, ChronoUnit.DAYS); // 1 year

        QrToken qrToken = new QrToken(user.getStudioId(), event.getClientId(), event.getId(), token, token, expiresAt);
        QrToken saved = qrTokenRepository.save(qrToken);

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "CREATE_QR",
                "QR_TOKEN",
                saved.getId(),
                "Generated QR gallery access token for event '" + event.getEventName() + "'",
                null
        );

        return enrichQrDto(saved, event);
    }

    @Transactional
    public QrResponseDto regenerateQr(UserPrincipal user, Long id) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can regenerate QR tokens");
        }

        QrToken qrToken = qrTokenRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("QR Token not found with id: " + id));

        qrToken.setStatus(QrTokenStatus.DISABLED);
        qrTokenRepository.save(qrToken);

        String newToken = generateSecureToken();
        Instant expiresAt = Instant.now().plus(365, ChronoUnit.DAYS);

        QrToken newQr = new QrToken(user.getStudioId(), qrToken.getClientId(), qrToken.getEventId(), newToken, newToken, expiresAt);
        QrToken saved = qrTokenRepository.save(newQr);

        Event event = eventRepository.findById(saved.getEventId()).orElse(null);

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "REGENERATE_QR",
                "QR_TOKEN",
                saved.getId(),
                "Regenerated QR gallery access token for event '" + (event != null ? event.getEventName() : "Event #" + saved.getEventId()) + "'",
                null
        );

        return enrichQrDto(saved, event);
    }

    @Transactional
    public void disableQr(UserPrincipal user, Long id) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can disable QR tokens");
        }

        QrToken qrToken = qrTokenRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("QR Token not found with id: " + id));

        qrToken.setStatus(QrTokenStatus.DISABLED);
        qrTokenRepository.save(qrToken);

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "DISABLE_QR",
                "QR_TOKEN",
                id,
                "Deactivated QR gallery access token #" + id,
                null
        );
    }

    @Transactional
    public void deleteQr(UserPrincipal user, Long id) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can delete QR tokens");
        }

        QrToken qrToken = qrTokenRepository.findByIdAndStudioId(id, user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("QR Token not found with id: " + id));

        qrTokenRepository.delete(qrToken);

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "DELETE_QR",
                "QR_TOKEN",
                id,
                "Permanently deleted QR gallery access token #" + id,
                null
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<QrResponseDto> getQrList(UserPrincipal user, Pageable pageable) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can view QR token directory");
        }

        Page<QrToken> page = qrTokenRepository.findByStudioId(user.getStudioId(), pageable);
        List<QrResponseDto> dtos = page.getContent().stream()
                .map(t -> {
                    Event event = eventRepository.findById(t.getEventId()).orElse(null);
                    return enrichQrDto(t, event);
                })
                .collect(Collectors.toList());

        return new PageResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public byte[] generateQrCodeImage(Long qrId, int width, int height) throws IOException {
        return generateQrCodeImage(qrId, width, height, "auto");
    }

    public byte[] generateQrCodeImage(Long qrId, int width, int height, String type) throws IOException {
        QrToken token = qrTokenRepository.findById(qrId)
                .orElseThrow(() -> new ResourceNotFoundException("QR Token not found with id: " + qrId));

        boolean hasCommercial = albumRepository.findByEventIdAndStudioId(token.getEventId(), token.getStudioId())
                .stream().anyMatch(a -> Boolean.TRUE.equals(a.getIsCommercial()));

        String targetUrl;
        if ("host".equalsIgnoreCase(type)) {
            targetUrl = galleryBaseUrl + token.getTokenValue();
        } else if ("guest".equalsIgnoreCase(type) || hasCommercial) {
            // Direct guest standee QR opens the guest candid store directly!
            targetUrl = galleryBaseUrl + token.getTokenValue() + "/guest";
        } else {
            targetUrl = galleryBaseUrl + token.getTokenValue();
        }

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(targetUrl, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new IOException("Failed to generate QR code image", e);
        }
    }

    @Transactional
    public ClientGalleryDto getGalleryByToken(String tokenValue) {
        QrToken qrToken = qrTokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired gallery access token"));

        if (qrToken.getStatus() != QrTokenStatus.ACTIVE || qrToken.isExpired()) {
            throw new ForbiddenException("This gallery QR token is no longer active");
        }

        // Update last used timestamp
        qrToken.setLastUsedAt(Instant.now());
        qrTokenRepository.save(qrToken);

        Studio studio = studioRepository.findById(qrToken.getStudioId()).orElse(null);
        Client client = clientRepository.findById(qrToken.getClientId()).orElse(null);
        Event event = eventRepository.findById(qrToken.getEventId()).orElse(null);

        if (event == null || client == null || studio == null) {
            throw new ResourceNotFoundException("Gallery content could not be located");
        }

        // Calculate client payment metrics and access gating
        BigDecimal totalAmount = client.getTotalAmount() != null ? client.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal amountPaid = paymentRepository.sumAmountByClientIdAndStudioId(client.getId(), studio.getId());
        if (amountPaid == null) {
            amountPaid = BigDecimal.ZERO;
        }

        BigDecimal balanceAmount = totalAmount.subtract(amountPaid).max(BigDecimal.ZERO);
        double paidPercentage = 100.0;
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            paidPercentage = Math.min(100.0, (amountPaid.doubleValue() / totalAmount.doubleValue()) * 100.0);
            paidPercentage = Math.round(paidPercentage * 10.0) / 10.0;
        }

        // Tier Rules: >= 50% photos unlocked; >= 80% full access (videos unlocked); 100% full download access
        boolean photoLocked = paidPercentage < 50.0;
        boolean videoLocked = paidPercentage < 80.0;
        boolean downloadLocked = paidPercentage < 100.0;

        BigDecimal amountNeededForVideo = BigDecimal.ZERO.setScale(2);
        if (videoLocked && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal target80 = totalAmount.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);
            amountNeededForVideo = target80.subtract(amountPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal amountNeededForFull = balanceAmount;

        List<Album> albums = albumRepository.findByEventIdAndStudioId(event.getId(), studio.getId());
        List<ClientGalleryDto.GalleryAlbumDto> albumDtos = new ArrayList<>();
        int totalPhotos = 0;
        int totalVideos = 0;

        for (Album album : albums) {
            List<Media> mediaList = mediaRepository.findByAlbumIdAndStudioId(album.getId(), studio.getId());
            List<MediaResponseDto> mediaDtos = new ArrayList<>();

            for (Media m : mediaList) {
                MediaResponseDto mDto = MediaResponseDto.fromEntity(m);
                boolean isLocked = false;
                if (m.getMediaType() == MediaType.VIDEO && videoLocked) {
                    isLocked = true;
                } else if (m.getMediaType() == MediaType.PHOTO && photoLocked) {
                    isLocked = true;
                }
                mDto.setLocked(isLocked);
                mDto.setDownloadLocked(downloadLocked);
                mediaDtos.add(mDto);

                if (m.getMediaType() == MediaType.PHOTO) totalPhotos++;
                else totalVideos++;
            }

            albumDtos.add(new ClientGalleryDto.GalleryAlbumDto(
                    album.getId(),
                    album.getName(),
                    album.getDescription(),
                    album.getCoverImageUrl(),
                    mediaDtos,
                    album.getIsCommercial(),
                    album.getPricePerPhoto(),
                    album.getAllowGuestPurchases()
            ));
        }

        ClientGalleryDto gallery = new ClientGalleryDto();
        gallery.setStudioName(studio.getName());
        gallery.setStudioLogoUrl(studio.getLogoUrl());
        gallery.setStudioPhone(studio.getPhone());
        gallery.setStudioEmail(studio.getEmail());
        gallery.setClientId(client.getId());
        gallery.setClientName(client.getName());
        gallery.setEventId(event.getId());
        gallery.setEventName(event.getEventName());
        gallery.setEventType(event.getEventType());
        gallery.setEventDate(event.getEventDate());
        gallery.setLocation(event.getLocation());
        gallery.setDescription(event.getDescription());
        gallery.setAlbums(albumDtos);
        gallery.setTotalPhotos(totalPhotos);
        gallery.setTotalVideos(totalVideos);
        gallery.setTotalAmount(totalAmount);
        gallery.setAmountPaid(amountPaid);
        gallery.setBalanceAmount(balanceAmount);
        gallery.setPaidPercentage(paidPercentage);
        gallery.setPhotoLocked(photoLocked);
        gallery.setVideoLocked(videoLocked);
        gallery.setDownloadLocked(downloadLocked);
        gallery.setAmountNeededForVideoAccess(amountNeededForVideo);
        gallery.setAmountNeededForDownload(balanceAmount);
        gallery.setAmountNeededForFullAccess(amountNeededForFull);
        gallery.setCurrency(studio.getCurrency() != null ? studio.getCurrency() : "INR");

        // Fetch client payments for invoice downloading
        List<Payment> clientPayments = paymentRepository.findByClientIdAndStudioIdOrderByPaymentDateDesc(client.getId(), studio.getId());
        List<ClientGalleryDto.GalleryPaymentDto> paymentDtos = clientPayments.stream()
                .map(p -> {
                    String inv = p.getInvoiceNumber();
                    if (inv == null || inv.isBlank()) {
                        int yr = p.getPaymentDate() != null ? p.getPaymentDate().getYear() : LocalDate.now().getYear();
                        long pid = p.getId() != null ? p.getId() : 1L;
                        inv = String.format("INV-%04d-%05d", yr, pid);
                    }
                    return new ClientGalleryDto.GalleryPaymentDto(
                            p.getId(),
                            inv,
                            p.getAmount(),
                            p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "OTHER",
                            p.getPaymentReference(),
                            p.getPaymentDate(),
                            p.getNotes()
                    );
                })
                .collect(Collectors.toList());
        gallery.setPayments(paymentDtos);

        return gallery;
    }

    public void downloadAlbumAsZip(String tokenValue, Long albumId, HttpServletResponse response) throws IOException {
        QrToken qrToken = qrTokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired gallery access token"));

        if (qrToken.getStatus() != QrTokenStatus.ACTIVE || qrToken.isExpired()) {
            throw new ForbiddenException("This gallery QR token is no longer active");
        }

        Client client = clientRepository.findById(qrToken.getClientId()).orElse(null);
        if (client == null) {
            throw new ResourceNotFoundException("Client not found for gallery token");
        }

        BigDecimal totalAmount = client.getTotalAmount() != null ? client.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal amountPaid = paymentRepository.sumAmountByClientIdAndStudioId(client.getId(), qrToken.getStudioId());
        if (amountPaid == null) amountPaid = BigDecimal.ZERO;

        BigDecimal target100 = totalAmount.setScale(2, RoundingMode.HALF_UP);
        if (amountPaid.compareTo(target100) < 0) {
            throw new ForbiddenException("Please complete 100% of the payment to download photos, videos, or albums.");
        }

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + albumId));

        if (!album.getEventId().equals(qrToken.getEventId())) {
            throw new ForbiddenException("Album does not belong to this event");
        }

        if (Boolean.TRUE.equals(album.getIsCommercial())) {
            throw new ForbiddenException("Commercial guest candid albums cannot be downloaded via ceremony ZIP. Guests must purchase individual candid photos via the Guest Candid Store.");
        }

        List<Media> mediaList = mediaRepository.findByAlbumIdAndStudioId(album.getId(), qrToken.getStudioId());

        String safeAlbumName = album.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + safeAlbumName + "_Album.zip\"");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            Set<String> addedNames = new HashSet<>();
            for (Media m : mediaList) {
                if (m.getFilePath() == null) continue;
                File f = new File(m.getFilePath());
                if (!f.exists()) continue;

                String entryName = m.getFileName() != null ? m.getFileName() : f.getName();
                if (addedNames.contains(entryName)) {
                    entryName = m.getId() + "_" + entryName;
                }
                addedNames.add(entryName);

                ZipEntry zipEntry = new ZipEntry(entryName);
                zipOut.putNextEntry(zipEntry);
                Files.copy(f.toPath(), zipOut);
                zipOut.closeEntry();
            }
            zipOut.finish();
        }
    }

    @Transactional
    public ClientGalleryDto processGalleryPayment(GalleryPaymentRequest request) {
        QrToken qrToken = qrTokenRepository.findByTokenValue(request.getTokenValue())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired gallery access token"));

        if (qrToken.getStatus() != QrTokenStatus.ACTIVE || qrToken.isExpired()) {
            throw new ForbiddenException("This gallery QR token is no longer active");
        }

        Client client = clientRepository.findById(qrToken.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment amount must be greater than zero");
        }

        Payment payment = new Payment();
        payment.setStudioId(qrToken.getStudioId());
        payment.setClientId(qrToken.getClientId());
        payment.setEventId(qrToken.getEventId());
        payment.setAmount(request.getAmount());
        com.turtledot.studio.payment.PaymentMethod method = com.turtledot.studio.payment.PaymentMethod.CARD;
        if (request.getPaymentMethod() != null) {
            try {
                method = com.turtledot.studio.payment.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase().trim());
            } catch (Exception ignored) {
                method = com.turtledot.studio.payment.PaymentMethod.OTHER;
            }
        }
        payment.setPaymentMethod(method);
        payment.setPaymentReference(request.getPaymentReference() != null ? request.getPaymentReference() : "PAY-" + System.currentTimeMillis());
        payment.setPaymentDate(LocalDate.now());
        payment.setNotes("Automated gallery payment (" + (request.getTierTarget() != null ? request.getTierTarget() : "Unlock Access") + ")");
        Payment saved = paymentRepository.save(payment);
        if (saved.getInvoiceNumber() == null || saved.getInvoiceNumber().isBlank()) {
            int year = saved.getPaymentDate() != null ? saved.getPaymentDate().getYear() : LocalDate.now().getYear();
            saved.setInvoiceNumber(String.format("INV-%04d-%05d", year, saved.getId()));
            paymentRepository.save(saved);
        }

        return getGalleryByToken(request.getTokenValue());
    }

    @Transactional(readOnly = true)
    public byte[] generateGalleryPaymentInvoicePdf(String tokenValue, Long paymentId) {
        QrToken qrToken = qrTokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired gallery access token"));

        if (qrToken.getStatus() != QrTokenStatus.ACTIVE || qrToken.isExpired()) {
            throw new ForbiddenException("This gallery QR token is no longer active");
        }

        Client client = clientRepository.findById(qrToken.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        Payment payment;
        if (paymentId != null) {
            payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
            if (!payment.getClientId().equals(client.getId())) {
                throw new ForbiddenException("Payment does not belong to this gallery client");
            }
        } else {
            List<Payment> payments = paymentRepository.findByClientIdAndStudioIdOrderByPaymentDateDesc(client.getId(), qrToken.getStudioId());
            if (payments.isEmpty()) {
                throw new ResourceNotFoundException("No payment records found for this client to generate invoice");
            }
            payment = payments.get(0);
        }

        Studio studio = studioRepository.findById(payment.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Event event = payment.getEventId() != null ? eventRepository.findById(payment.getEventId()).orElse(null) : null;
        BigDecimal totalPaid = paymentRepository.sumAmountByClientIdAndStudioId(client.getId(), studio.getId());

        return invoicePdfService.generatePaymentInvoicePdf(payment, studio, client, event, totalPaid);
    }

    @Transactional
    public com.turtledot.studio.qr.dto.GuestPurchaseResponseDto purchaseGuestPhoto(String tokenValue, com.turtledot.studio.qr.dto.GuestPurchaseRequest request) {
        QrToken qrToken = qrTokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired gallery access token"));

        if (qrToken.getStatus() != QrTokenStatus.ACTIVE || qrToken.isExpired()) {
            throw new ForbiddenException("This gallery QR token is no longer active");
        }

        Media media = mediaRepository.findById(request.getMediaId())
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + request.getMediaId()));

        Album album = albumRepository.findById(media.getAlbumId())
                .orElseThrow(() -> new ResourceNotFoundException("Album not found for media"));

        if (!album.getEventId().equals(qrToken.getEventId())) {
            throw new ForbiddenException("Media does not belong to this event");
        }

        if (Boolean.FALSE.equals(album.getAllowGuestPurchases())) {
            throw new ForbiddenException("Purchases are not enabled for this album");
        }

        BigDecimal price = album.getPricePerPhoto();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            price = request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0 
                    ? request.getAmount() 
                    : new BigDecimal("49.00");
        }

        String ref = request.getPaymentReference();
        if (ref == null || ref.isBlank()) {
            ref = "GUEST-PAY-" + System.currentTimeMillis();
        }

        GuestPurchase purchase = new GuestPurchase(
                qrToken.getStudioId(),
                qrToken.getEventId(),
                album.getId(),
                media.getId(),
                tokenValue,
                request.getGuestName() != null ? request.getGuestName() : "Guest Attendee",
                request.getGuestPhone(),
                request.getGuestEmail(),
                price,
                request.getPaymentMethod() != null ? request.getPaymentMethod() : "UPI",
                ref
        );

        GuestPurchase saved = guestPurchaseRepository.save(purchase);

        com.turtledot.studio.qr.dto.GuestPurchaseResponseDto response = new com.turtledot.studio.qr.dto.GuestPurchaseResponseDto();
        response.setPurchaseId(saved.getId());
        response.setMediaId(media.getId());
        response.setMediaFileName(media.getFileName());
        response.setAmount(saved.getAmount());
        response.setGuestName(saved.getGuestName());
        response.setGuestPhone(saved.getGuestPhone());
        response.setPaymentMethod(saved.getPaymentMethod());
        response.setPaymentReference(saved.getPaymentReference());
        response.setDownloadUrl("/api/qr/gallery/" + tokenValue + "/photos/" + media.getId() + "/download?purchaseId=" + saved.getId());
        response.setPurchasedAt(saved.getCreatedAt());

        return response;
    }

    @Transactional(readOnly = true)
    public List<Long> getGuestPurchasedMediaIds(String tokenValue, String phone) {
        QrToken qrToken = qrTokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired gallery access token"));

        List<GuestPurchase> purchases;
        if (phone != null && !phone.isBlank()) {
            purchases = guestPurchaseRepository.findByQrTokenAndGuestPhone(tokenValue, phone.trim());
        } else {
            purchases = guestPurchaseRepository.findByQrToken(tokenValue);
        }

        return purchases.stream()
                .map(GuestPurchase::getMediaId)
                .distinct()
                .collect(Collectors.toList());
    }

    public void downloadGuestPurchasedPhoto(String tokenValue, Long mediaId, Long purchaseId, HttpServletResponse response) throws IOException {
        QrToken qrToken = qrTokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired gallery access token"));

        if (qrToken.getStatus() != QrTokenStatus.ACTIVE || qrToken.isExpired()) {
            throw new ForbiddenException("This gallery QR token is no longer active");
        }

        // Validate purchase
        boolean authorized = false;
        if (purchaseId != null) {
            Optional<GuestPurchase> purchaseOpt = guestPurchaseRepository.findByIdAndQrToken(purchaseId, tokenValue);
            if (purchaseOpt.isPresent() && purchaseOpt.get().getMediaId().equals(mediaId)) {
                authorized = true;
            }
        }
        if (!authorized) {
            authorized = guestPurchaseRepository.existsByQrTokenAndMediaId(tokenValue, mediaId);
        }

        if (!authorized) {
            throw new ForbiddenException("Photo download is locked. Please complete the purchase to unlock your unwatermarked high-resolution copy.");
        }

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        if (media.getFilePath() == null) {
            throw new ResourceNotFoundException("Media file path not recorded");
        }

        File file = new File(media.getFilePath());
        if (!file.exists()) {
            throw new ResourceNotFoundException("Media file not available on disk");
        }

        String safeFileName = media.getFileName() != null ? media.getFileName() : ("Candid_Photo_" + mediaId + ".jpg");
        String mime = media.getMimeType() != null && !media.getMimeType().isBlank() ? media.getMimeType() : "image/jpeg";

        response.setContentType(mime);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + safeFileName + "\"");
        response.setContentLengthLong(file.length());

        try {
            Files.copy(file.toPath(), response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new IOException("Failed streaming guest photo download", e);
        }
    }

    private QrResponseDto enrichQrDto(QrToken token, Event event) {
        Client client = event != null ? clientRepository.findById(event.getClientId()).orElse(null) : null;

        boolean hasCommercial = false;
        if (event != null) {
            hasCommercial = albumRepository.findByEventIdAndStudioId(event.getId(), token.getStudioId())
                    .stream().anyMatch(a -> Boolean.TRUE.equals(a.getIsCommercial()));
        }

        QrResponseDto dto = new QrResponseDto();
        dto.setId(token.getId());
        dto.setEventId(token.getEventId());
        dto.setEventName(event != null ? event.getEventName() : "Unknown Event");
        dto.setClientId(token.getClientId());
        dto.setClientName(client != null ? client.getName() : "Unknown Client");
        dto.setTokenValue(token.getTokenValue());
        dto.setGalleryUrl(galleryBaseUrl + token.getTokenValue());
        dto.setGuestGalleryUrl(galleryBaseUrl + token.getTokenValue() + "/guest");
        dto.setHasCommercialAlbums(hasCommercial);
        dto.setStatus(token.getStatus());
        dto.setCreatedAt(token.getCreatedAt());
        dto.setLastUsedAt(token.getLastUsedAt());
        return dto;
    }

    private String generateSecureToken() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(TOKEN_CHARS.charAt(RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return sb.toString();
    }
}
