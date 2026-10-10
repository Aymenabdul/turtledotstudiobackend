package com.turtledot.studio.auth;

import com.turtledot.studio.auth.dto.AuthResponse;
import com.turtledot.studio.auth.dto.LoginRequest;
import com.turtledot.studio.auth.dto.RefreshTokenRequest;
import com.turtledot.studio.auth.dto.UserSummaryDto;
import com.turtledot.studio.exception.ResourceNotFoundException;
import com.turtledot.studio.exception.UnauthorizedException;
import com.turtledot.studio.security.JwtTokenProvider;
import com.turtledot.studio.security.UserPrincipal;
import com.turtledot.studio.studio.Studio;
import com.turtledot.studio.studio.StudioRepository;
import com.turtledot.studio.user.Role;
import com.turtledot.studio.user.User;
import com.turtledot.studio.user.UserRepository;
import com.turtledot.studio.user.UserStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.turtledot.studio.audit.AuditService;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final StudioRepository studioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UserRepository userRepository,
                       StudioRepository studioRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JdbcTemplate jdbcTemplate,
                       AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.studioRepository = studioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("Account is disabled. Please contact your studio administrator.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        // Store refresh token
        Instant expiresAt = Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs());
        RefreshToken rt = new RefreshToken(user.getId(), refreshToken, expiresAt);
        refreshTokenRepository.save(rt);

        Studio studio = studioRepository.findById(user.getStudioId())
                .orElse(new Studio("Studio", "studio", null, null));

        UserSummaryDto userSummary = new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                studio.getId(),
                studio.getName(),
                studio.getSlug(),
                studio.getLogoUrl(),
                studio.getCurrency()
        );
        userSummary.setPermissions(resolvePermissions(user));

        auditService.log(
                user.getStudioId(),
                user.getId(),
                "USER_LOGIN",
                "USER",
                user.getId(),
                "User '" + user.getFullName() + "' (" + user.getEmail() + ") logged in successfully",
                null
        );

        return new AuthResponse(accessToken, refreshToken, 900L, userSummary);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenStr = request.getRefreshToken();
        if (!tokenProvider.validateToken(tokenStr)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenStr)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token has expired or been revoked");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is disabled");
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

        Studio studio = studioRepository.findById(user.getStudioId())
                .orElse(new Studio("Studio", "studio", null, null));

        UserSummaryDto userSummary = new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                studio.getId(),
                studio.getName(),
                studio.getSlug(),
                studio.getLogoUrl(),
                studio.getCurrency()
        );
        userSummary.setPermissions(resolvePermissions(user));

        return new AuthResponse(newAccessToken, tokenStr, 900L, userSummary);
    }

    private List<String> resolvePermissions(User user) {
        if (user.getRole() == Role.ADMIN) {
            return List.of(
                    "CLIENT_VIEW", "CLIENT_EDIT", "ALBUM_CREATE",
                    "PHOTO_UPLOAD", "PHOTO_DELETE", "VIDEO_UPLOAD",
                    "MEDIA_DOWNLOAD", "ALBUM_DELETE", "SHARE_MANAGE"
            );
        }
        if (user.getRole() == Role.EMPLOYEE) {
            try {
                Long empId = jdbcTemplate.queryForObject("SELECT id FROM employees WHERE user_id = ?", Long.class, user.getId());
                if (empId != null) {
                    return jdbcTemplate.queryForList("SELECT permission FROM employee_permissions WHERE employee_id = ?", String.class, empId);
                }
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    @Transactional
    public void logout(Long userId) {
        if (userId != null) {
            refreshTokenRepository.revokeAllByUserId(userId);
        }
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userPrincipal.getId()));

        Studio studio = studioRepository.findById(user.getStudioId())
                .orElse(new Studio("Studio", "studio", null, null));

        return new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                studio.getId(),
                studio.getName(),
                studio.getSlug(),
                studio.getLogoUrl(),
                studio.getCurrency()
        );
    }
}
