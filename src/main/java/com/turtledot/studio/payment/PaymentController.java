package com.turtledot.studio.payment;

import com.turtledot.studio.common.ApiResponse;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.payment.dto.CreatePaymentRequest;
import com.turtledot.studio.payment.dto.PaymentResponseDto;
import com.turtledot.studio.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Endpoints for managing client payments, installments, and studio revenue")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Record a client payment or installment")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> recordPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponseDto response = paymentService.recordPayment(userPrincipal, request);
        return ResponseEntity.ok(ApiResponse.success("Payment recorded successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all studio payments (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponseDto>>> getStudioPayments(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paymentDate", "id"));
        PageResponse<PaymentResponseDto> result = paymentService.getStudioPayments(userPrincipal, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get all payment records for a specific client")
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getClientPayments(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long clientId
    ) {
        List<PaymentResponseDto> result = paymentService.getClientPayments(userPrincipal, clientId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping(value = "/{id}/invoice", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CLIENT')")
    @Operation(summary = "Download official GST tax invoice for a payment as PDF")
    public ResponseEntity<byte[]> downloadPaymentInvoicePdf(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        byte[] pdfBytes = paymentService.generatePaymentInvoicePdf(userPrincipal, id);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("GST_Invoice_INV_" + id + ".pdf")
                .build());
        headers.setContentLength(pdfBytes.length);
        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete or reverse a recorded payment (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deletePayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        paymentService.deletePayment(userPrincipal, id);
        return ResponseEntity.ok(ApiResponse.success("Payment deleted successfully", null));
    }
}
