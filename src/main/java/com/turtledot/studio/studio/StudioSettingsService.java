package com.turtledot.studio.studio;

import com.turtledot.studio.audit.AuditService;
import com.turtledot.studio.exception.ResourceNotFoundException;
import com.turtledot.studio.security.AuthorizationService;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.studio.dto.StudioSettingsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudioSettingsService {

    private final StudioRepository studioRepository;
    private final AuthorizationService authService;
    private final AuditService auditService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public StudioSettingsService(StudioRepository studioRepository,
                                 AuthorizationService authService,
                                 AuditService auditService,
                                 org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.studioRepository = studioRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public StudioSettingsDto getSettings(UserPrincipal user) {
        Studio studio = studioRepository.findById(user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found with id: " + user.getStudioId()));
        return StudioSettingsDto.fromEntity(studio);
    }

    @Transactional
    public StudioSettingsDto updateSettings(UserPrincipal user, StudioSettingsDto dto) {
        Studio studio = studioRepository.findById(user.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found with id: " + user.getStudioId()));

        String oldCurrency = studio.getCurrency() != null ? studio.getCurrency() : "USD";
        String newCurrency = dto.getCurrency() != null && !dto.getCurrency().isBlank()
                ? dto.getCurrency().trim().toUpperCase()
                : oldCurrency;

        if (Boolean.TRUE.equals(dto.getConvertAmounts()) && !oldCurrency.equalsIgnoreCase(newCurrency)) {
            java.math.BigDecimal rate = dto.getExchangeRate();
            if (rate == null || rate.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                rate = resolveDefaultRate(oldCurrency, newCurrency);
            }
            if (rate != null && rate.compareTo(java.math.BigDecimal.ZERO) > 0) {
                jdbcTemplate.update("UPDATE clients SET total_amount = ROUND(total_amount * ?, 2) WHERE studio_id = ?", rate, studio.getId());
                jdbcTemplate.update("UPDATE payments SET amount = ROUND(amount * ?, 2) WHERE studio_id = ?", rate, studio.getId());
            }
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            studio.setName(dto.getName().trim());
        }
        if (dto.getPhone() != null) {
            studio.setPhone(dto.getPhone().trim());
        }
        if (dto.getEmail() != null) {
            studio.setEmail(dto.getEmail().trim());
        }
        if (dto.getAddress() != null) {
            studio.setAddress(dto.getAddress().trim());
        }
        if (dto.getWebsite() != null) {
            studio.setWebsite(dto.getWebsite().trim());
        }
        studio.setCurrency(newCurrency);
        if (dto.getTimezone() != null && !dto.getTimezone().isBlank()) {
            studio.setTimezone(dto.getTimezone().trim());
        }
        if (dto.getLogoUrl() != null) {
            studio.setLogoUrl(dto.getLogoUrl().trim());
        }
        if (dto.getMaxUploadSizeMb() != null && dto.getMaxUploadSizeMb() > 0) {
            studio.setMaxUploadSizeMb(dto.getMaxUploadSizeMb());
        }
        if (dto.getGstin() != null) {
            studio.setGstin(dto.getGstin().trim().toUpperCase());
        }
        if (dto.getPanNumber() != null) {
            studio.setPanNumber(dto.getPanNumber().trim().toUpperCase());
        }
        if (dto.getStateName() != null) {
            studio.setStateName(dto.getStateName().trim());
        }
        if (dto.getStateCode() != null) {
            studio.setStateCode(dto.getStateCode().trim());
        }
        if (dto.getGstRatePercent() != null && dto.getGstRatePercent().compareTo(java.math.BigDecimal.ZERO) >= 0) {
            studio.setGstRatePercent(dto.getGstRatePercent());
        }

        Studio updated = studioRepository.save(studio);

        auditService.log(
                updated.getId(),
                user.getId(),
                "UPDATE_SETTINGS",
                "STUDIO",
                updated.getId(),
                "Updated studio brand profile and settings",
                null
        );

        return StudioSettingsDto.fromEntity(updated);
    }

    private java.math.BigDecimal resolveDefaultRate(String from, String to) {
        String f = from.toUpperCase();
        String t = to.toUpperCase();
        if (f.equals(t)) return java.math.BigDecimal.ONE;

        // USD to INR = 83.00, INR to USD = 0.01205
        if ("USD".equals(f) && "INR".equals(t)) return new java.math.BigDecimal("83.00");
        if ("INR".equals(f) && "USD".equals(t)) return new java.math.BigDecimal("0.012048");
        if ("EUR".equals(f) && "INR".equals(t)) return new java.math.BigDecimal("90.00");
        if ("INR".equals(f) && "EUR".equals(t)) return new java.math.BigDecimal("0.011111");
        if ("GBP".equals(f) && "INR".equals(t)) return new java.math.BigDecimal("108.00");
        if ("INR".equals(f) && "GBP".equals(t)) return new java.math.BigDecimal("0.009259");
        if ("CAD".equals(f) && "INR".equals(t)) return new java.math.BigDecimal("61.00");
        if ("AUD".equals(f) && "INR".equals(t)) return new java.math.BigDecimal("55.00");

        return java.math.BigDecimal.ONE;
    }
}
