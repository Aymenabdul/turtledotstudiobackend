package com.turtledot.studio.dashboard;

import com.turtledot.studio.client.Client;
import com.turtledot.studio.client.ClientRepository;
import com.turtledot.studio.client.ClientStatus;
import com.turtledot.studio.dashboard.dto.DashboardStatsDto;
import com.turtledot.studio.dashboard.dto.MonthlyRevenueDto;
import com.turtledot.studio.employee.Employee;
import com.turtledot.studio.employee.EmployeeClientAccessRepository;
import com.turtledot.studio.employee.EmployeeRepository;
import com.turtledot.studio.event.Event;
import com.turtledot.studio.event.EventRepository;
import com.turtledot.studio.event.dto.EventResponseDto;
import com.turtledot.studio.media.MediaRepository;
import com.turtledot.studio.media.MediaType;
import com.turtledot.studio.payment.Payment;
import com.turtledot.studio.payment.PaymentRepository;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.studio.Studio;
import com.turtledot.studio.studio.StudioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final PaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final EventRepository eventRepository;
    private final MediaRepository mediaRepository;
    private final StudioRepository studioRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeClientAccessRepository accessRepository;

    public DashboardService(PaymentRepository paymentRepository,
                            ClientRepository clientRepository,
                            EventRepository eventRepository,
                            MediaRepository mediaRepository,
                            StudioRepository studioRepository,
                            EmployeeRepository employeeRepository,
                            EmployeeClientAccessRepository accessRepository) {
        this.paymentRepository = paymentRepository;
        this.clientRepository = clientRepository;
        this.eventRepository = eventRepository;
        this.mediaRepository = mediaRepository;
        this.studioRepository = studioRepository;
        this.employeeRepository = employeeRepository;
        this.accessRepository = accessRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getStudioStats(UserPrincipal user) {
        Long studioId = user.getStudioId();

        BigDecimal totalRevenue = paymentRepository.sumTotalRevenueByStudioId(studioId);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        BigDecimal totalAmount = clientRepository.sumTotalAmountByStudioId(studioId);
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;

        BigDecimal totalPending = totalAmount.subtract(totalRevenue);
        if (totalPending.compareTo(BigDecimal.ZERO) < 0) {
            totalPending = BigDecimal.ZERO;
        }

        long totalClients = clientRepository.countByStudioIdAndStatus(studioId, ClientStatus.ACTIVE);
        long totalEvents = eventRepository.countByStudioId(studioId);
        long totalPhotos = mediaRepository.countByStudioIdAndMediaType(studioId, MediaType.PHOTO);
        long totalVideos = mediaRepository.countByStudioIdAndMediaType(studioId, MediaType.VIDEO);
        long totalStorageBytes = mediaRepository.getTotalStorageUsedBytes(studioId);

        Studio studio = studioRepository.findById(studioId).orElse(null);
        long quotaBytes = 107374182400L; // 100GB default

        DashboardStatsDto dto = new DashboardStatsDto();
        dto.setTotalRevenue(totalRevenue);
        dto.setTotalPending(totalPending);
        dto.setTotalClients(totalClients);
        dto.setTotalEvents(totalEvents);
        dto.setTotalPhotos(totalPhotos);
        dto.setTotalVideos(totalVideos);
        dto.setTotalStorageBytes(totalStorageBytes);
        dto.setStorageQuotaBytes(quotaBytes);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<MonthlyRevenueDto> getMonthlyRevenue(UserPrincipal user) {
        Long studioId = user.getStudioId();
        LocalDate now = LocalDate.now();
        LocalDate sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1);

        List<Payment> payments = paymentRepository.findByStudioIdAndDateRange(studioId, sixMonthsAgo, now);

        // Group payments by Month-Year
        Map<String, MonthlyRevenueDto> monthlyMap = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthName = monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            String key = monthDate.getYear() + "-" + String.format("%02d", monthDate.getMonthValue());
            monthlyMap.put(key, new MonthlyRevenueDto(monthName, monthDate.getYear(), BigDecimal.ZERO, 0));
        }

        for (Payment p : payments) {
            LocalDate pDate = p.getPaymentDate();
            String key = pDate.getYear() + "-" + String.format("%02d", pDate.getMonthValue());
            MonthlyRevenueDto entry = monthlyMap.get(key);
            if (entry != null) {
                entry.setRevenue(entry.getRevenue().add(p.getAmount()));
                entry.setPaymentCount(entry.getPaymentCount() + 1);
            }
        }

        return new ArrayList<>(monthlyMap.values());
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getUpcomingEvents(UserPrincipal user) {
        Long studioId = user.getStudioId();
        List<Event> events = eventRepository.findUpcomingEvents(studioId, LocalDate.now(), PageRequest.of(0, 5));

        Map<Long, String> clientNames = new HashMap<>();
        return events.stream()
                .map(e -> {
                    String clientName = clientNames.computeIfAbsent(e.getClientId(), id ->
                            clientRepository.findById(id).map(Client::getName).orElse("Client"));
                    return EventResponseDto.fromEntity(e, clientName);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEmployeeDashboardStats(UserPrincipal user) {
        Long studioId = user.getStudioId();
        Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);

        long assignedClientsCount = 0;
        if (employee != null) {
            assignedClientsCount = accessRepository.findByEmployeeId(employee.getId()).stream()
                    .map(com.turtledot.studio.employee.EmployeeClientAccess::getClientId)
                    .distinct()
                    .filter(cId -> clientRepository.findById(cId)
                            .map(c -> c.getStatus() != ClientStatus.DELETED)
                            .orElse(false))
                    .count();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("assignedClientsCount", assignedClientsCount);
        stats.put("totalStudioPhotos", mediaRepository.countByStudioIdAndMediaType(studioId, MediaType.PHOTO));
        stats.put("totalStudioVideos", mediaRepository.countByStudioIdAndMediaType(studioId, MediaType.VIDEO));

        return stats;
    }
}
