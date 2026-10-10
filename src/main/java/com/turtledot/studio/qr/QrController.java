package com.turtledot.studio.qr;

import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.qr.dto.ClientGalleryDto;
import com.turtledot.studio.qr.dto.GalleryPaymentRequest;
import com.turtledot.studio.qr.dto.QrResponseDto;
import com.turtledot.studio.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/qr")
@Tag(name = "QR Gallery", description = "Endpoints for generating secure event QR codes and delivering client photo galleries")
public class QrController {

    private final QrService qrService;

    public QrController(QrService qrService) {
        this.qrService = qrService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate a secure QR gallery token for an event (Admin only)")
    public ResponseEntity<ApiResponse<QrResponseDto>> generateQr(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Long eventId
    ) {
        QrResponseDto response = qrService.generateQrForEvent(userPrincipal, eventId);
        return ResponseEntity.ok(ApiResponse.success("QR code generated", response));
    }

    @PostMapping("/{id}/regenerate")
    @Operation(summary = "Regenerate a QR gallery token (Admin only)")
    public ResponseEntity<ApiResponse<QrResponseDto>> regenerateQr(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        QrResponseDto response = qrService.regenerateQr(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success("QR code regenerated", response));
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable a QR gallery token (Admin only)")
    public ResponseEntity<ApiResponse<Void>> disableQr(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        qrService.disableQr(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success("QR code disabled", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a QR gallery token (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteQr(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        qrService.deleteQr(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success("QR code deleted successfully", null));
    }

    @GetMapping("/list")
    @Operation(summary = "List all QR tokens for studio (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<QrResponseDto>>> getQrList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        PageResponse<QrResponseDto> list = qrService.getQrList(userPrincipal, pageable);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Download or render high-resolution PNG QR code")
    public ResponseEntity<byte[]> getQrImage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "400") int size,
            @RequestParam(defaultValue = "auto") String type
    ) throws IOException {
        byte[] imageBytes = qrService.generateQrCodeImage(id, size, size, type);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"gallery_qr_" + id + "_" + type + ".png\"")
                .body(imageBytes);
    }

    @GetMapping("/gallery/{token}")
    @Operation(summary = "Public client gallery view accessed by scanning QR code")
    public ResponseEntity<ApiResponse<ClientGalleryDto>> getClientGallery(@PathVariable String token) {
        ClientGalleryDto gallery = qrService.getGalleryByToken(token);
        return ResponseEntity.ok(ApiResponse.success(gallery));
    }

    @PostMapping("/gallery/pay")
    @Operation(summary = "Automated online payment from client gallery to unlock access tiers")
    public ResponseEntity<ApiResponse<ClientGalleryDto>> payGallery(@Valid @RequestBody GalleryPaymentRequest request) {
        ClientGalleryDto updated = qrService.processGalleryPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment recorded successfully. Access updated.", updated));
    }

    @GetMapping("/gallery/{token}/albums/{albumId}/download")
    @Operation(summary = "Download all photos and videos in an album as a ZIP archive (Requires 100% payment)")
    public void downloadAlbum(
            @PathVariable String token,
            @PathVariable Long albumId,
            jakarta.servlet.http.HttpServletResponse response
    ) throws IOException {
        qrService.downloadAlbumAsZip(token, albumId, response);
    }

    @GetMapping(value = "/gallery/{token}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download latest official GST tax invoice for client gallery as PDF")
    public ResponseEntity<byte[]> downloadGalleryLatestInvoice(@PathVariable String token) {
        byte[] pdfBytes = qrService.generateGalleryPaymentInvoicePdf(token, null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("GST_Invoice_" + token + ".pdf")
                .build());
        headers.setContentLength(pdfBytes.length);
        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping(value = "/gallery/{token}/payments/{paymentId}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download specific payment GST tax invoice for client gallery as PDF")
    public ResponseEntity<byte[]> downloadGalleryPaymentInvoice(
            @PathVariable String token,
            @PathVariable Long paymentId
    ) {
        byte[] pdfBytes = qrService.generateGalleryPaymentInvoicePdf(token, paymentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("GST_Invoice_INV_" + paymentId + ".pdf")
                .build());
        headers.setContentLength(pdfBytes.length);
        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }

    @PostMapping("/gallery/{token}/photos/purchase")
    @Operation(summary = "Guest purchase an individual candid photo from an event album")
    public ResponseEntity<ApiResponse<com.turtledot.studio.qr.dto.GuestPurchaseResponseDto>> purchasePhoto(
            @PathVariable String token,
            @Valid @RequestBody com.turtledot.studio.qr.dto.GuestPurchaseRequest request
    ) {
        com.turtledot.studio.qr.dto.GuestPurchaseResponseDto response = qrService.purchaseGuestPhoto(token, request);
        return ResponseEntity.ok(ApiResponse.success("Photo purchase successful! Instant download unlocked.", response));
    }

    @GetMapping("/gallery/{token}/photos/purchased")
    @Operation(summary = "Get list of photo IDs purchased by guest")
    public ResponseEntity<ApiResponse<java.util.List<Long>>> getPurchasedPhotos(
            @PathVariable String token,
            @RequestParam(required = false) String phone
    ) {
        java.util.List<Long> purchased = qrService.getGuestPurchasedMediaIds(token, phone);
        return ResponseEntity.ok(ApiResponse.success(purchased));
    }

    @GetMapping("/gallery/{token}/photos/{mediaId}/download")
    @Operation(summary = "Download unwatermarked high-resolution photo after guest purchase")
    public void downloadPurchasedPhoto(
            @PathVariable String token,
            @PathVariable Long mediaId,
            @RequestParam(required = false) Long purchaseId,
            jakarta.servlet.http.HttpServletResponse response
    ) throws IOException {
        qrService.downloadGuestPurchasedPhoto(token, mediaId, purchaseId, response);
    }
}
