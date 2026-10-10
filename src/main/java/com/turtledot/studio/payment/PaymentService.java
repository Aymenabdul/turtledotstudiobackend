package com.turtledot.studio.payment;

import com.turtledot.studio.audit.AuditService;
import com.turtledot.studio.client.Client;
import com.turtledot.studio.client.ClientRepository;
import com.turtledot.studio.common.PageResponse;
import com.turtledot.studio.event.Event;
import com.turtledot.studio.event.EventRepository;
import com.turtledot.studio.exception.ResourceNotFoundException;
import com.turtledot.studio.exception.ValidationException;
import com.turtledot.studio.payment.dto.CreatePaymentRequest;
import com.turtledot.studio.payment.dto.PaymentResponseDto;
import com.turtledot.studio.security.AuthorizationService;
import com.turtledot.studio.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.turtledot.studio.studio.Studio;
import com.turtledot.studio.studio.StudioRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final EventRepository eventRepository;
    private final StudioRepository studioRepository;
    private final AuthorizationService authService;
    private final AuditService auditService;
    private final InvoicePdfService invoicePdfService;

    public PaymentService(PaymentRepository paymentRepository,
                          ClientRepository clientRepository,
                          EventRepository eventRepository,
                          StudioRepository studioRepository,
                          AuthorizationService authService,
                          AuditService auditService,
                          InvoicePdfService invoicePdfService) {
        this.paymentRepository = paymentRepository;
        this.clientRepository = clientRepository;
        this.eventRepository = eventRepository;
        this.studioRepository = studioRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.invoicePdfService = invoicePdfService;
    }

    @Transactional
    public PaymentResponseDto recordPayment(UserPrincipal user, CreatePaymentRequest req) {
        Client client = clientRepository.findById(req.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + req.getClientId()));

        authService.checkStudioAccess(user, client.getStudioId());
        authService.checkClientAccess(user, client.getId(), "CLIENT_VIEW");

        Event event = null;
        if (req.getEventId() != null) {
            event = eventRepository.findById(req.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + req.getEventId()));
            if (!event.getClientId().equals(client.getId())) {
                throw new ValidationException("Event does not belong to the specified client");
            }
        }

        Payment payment = new Payment();
        payment.setStudioId(client.getStudioId());
        payment.setClientId(client.getId());
        payment.setEventId(event != null ? event.getId() : null);
        payment.setAmount(req.getAmount());
        payment.setPaymentMethod(req.getPaymentMethod());
        payment.setPaymentReference(req.getPaymentReference());
        payment.setPaymentDate(req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now());
        payment.setNotes(req.getNotes());
        payment.setCreatedBy(user.getId());

        Payment saved = paymentRepository.save(payment);
        if (saved.getInvoiceNumber() == null || saved.getInvoiceNumber().isBlank()) {
            int year = saved.getPaymentDate() != null ? saved.getPaymentDate().getYear() : LocalDate.now().getYear();
            saved.setInvoiceNumber(String.format("INV-%04d-%05d", year, saved.getId()));
            saved = paymentRepository.save(saved);
        }

        auditService.log(
                client.getStudioId(),
                user.getId(),
                "RECORD_PAYMENT",
                "PAYMENT",
                saved.getId(),
                "Recorded payment of " + saved.getAmount() + " via " + saved.getPaymentMethod() + " for client " + client.getName(),
                null
        );

        String eventName = event != null ? event.getEventName() : null;
        return PaymentResponseDto.fromEntity(saved, client.getName(), eventName);
    }

    @Transactional(readOnly = true)
    public byte[] generatePaymentInvoicePdf(UserPrincipal user, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        Client client = clientRepository.findById(payment.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (user != null) {
            if (user.getRole() == com.turtledot.studio.user.Role.CLIENT) {
                if (client.getUserId() == null || !client.getUserId().equals(user.getId())) {
                    throw new com.turtledot.studio.exception.ForbiddenException("You do not have permission to access this invoice");
                }
            } else {
                authService.checkStudioAccess(user, payment.getStudioId());
            }
        }

        Studio studio = studioRepository.findById(payment.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found with id: " + payment.getStudioId()));

        Event event = payment.getEventId() != null ? eventRepository.findById(payment.getEventId()).orElse(null) : null;
        java.math.BigDecimal totalPaid = paymentRepository.sumAmountByClientIdAndStudioId(client.getId(), studio.getId());

        return invoicePdfService.generatePaymentInvoicePdf(payment, studio, client, event, totalPaid);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getClientPayments(UserPrincipal user, Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        authService.checkStudioAccess(user, client.getStudioId());
        authService.checkClientAccess(user, client.getId(), "CLIENT_VIEW");

        List<Payment> payments = paymentRepository.findByClientIdAndStudioIdOrderByPaymentDateDesc(clientId, client.getStudioId());

        Map<Long, Event> eventMap = eventRepository.findByStudioIdAndClientId(client.getStudioId(), clientId).stream()
                .collect(Collectors.toMap(Event::getId, e -> e, (e1, e2) -> e1));

        return payments.stream()
                .map(p -> {
                    Event ev = p.getEventId() != null ? eventMap.get(p.getEventId()) : null;
                    return PaymentResponseDto.fromEntity(p, client.getName(), ev != null ? ev.getEventName() : null);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponseDto> getStudioPayments(UserPrincipal user, Pageable pageable) {
        Page<Payment> page = paymentRepository.findByStudioId(user.getStudioId(), pageable);

        Map<Long, Client> clientMap = clientRepository.findByStudioId(user.getStudioId()).stream()
                .collect(Collectors.toMap(Client::getId, c -> c, (c1, c2) -> c1));

        Map<Long, Event> eventMap = eventRepository.findByStudioId(user.getStudioId()).stream()
                .collect(Collectors.toMap(Event::getId, e -> e, (e1, e2) -> e1));

        List<PaymentResponseDto> dtos = page.getContent().stream()
                .map(p -> {
                    Client c = clientMap.get(p.getClientId());
                    Event ev = p.getEventId() != null ? eventMap.get(p.getEventId()) : null;
                    String clientName = c != null ? c.getName() : "Unknown Client";
                    String eventName = ev != null ? ev.getEventName() : null;
                    return PaymentResponseDto.fromEntity(p, clientName, eventName);
                })
                .collect(Collectors.toList());

        return new PageResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public void deletePayment(UserPrincipal user, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        authService.checkStudioAccess(user, payment.getStudioId());

        paymentRepository.delete(payment);

        auditService.log(
                payment.getStudioId(),
                user.getId(),
                "DELETE_PAYMENT",
                "PAYMENT",
                paymentId,
                "Deleted payment of " + payment.getAmount() + " (ID: " + paymentId + ")",
                null
        );
    }
}
