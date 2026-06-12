package com.stubserver.backend.service;

import com.stubserver.backend.config.AppProperties;
import org.common.db.entity.RefreshToken;
import org.common.db.entity.User;
import com.stubserver.backend.exception.BadRequestException;
import com.stubserver.backend.exception.ForbiddenException;
import com.stubserver.backend.exception.NotFoundException;
import com.stubserver.backend.exception.UnauthorizedException;
import org.common.db.repository.RefreshTokenRepository;
import org.common.db.repository.UserRepository;
import com.stubserver.backend.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final long REFRESH_MAX_AGE_MS = 15L * 24 * 60 * 60 * 1000;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final AppProperties appProperties;
    private final MailService mailService;

    @Transactional
    public Map<String, Object> signIn(String username, String password,
                                       HttpServletRequest request, HttpServletResponse response) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Failed login - username not found: {}", username);
                    return new UnauthorizedException("Invalid credentials");
                });

        if (!tokenService.verifyPassword(password, user.getPassword())) {
            log.warn("Failed login - wrong password for username: {}", username);
            throw new UnauthorizedException("Invalid credentials");
        }

        refreshTokenRepository.deleteAllByUsername(user.getUsername());

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getUserrole());
        JwtService.RefreshTokenResult refresh = jwtService.generateRefreshToken(user.getUsername());

        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + REFRESH_MAX_AGE_MS);
        String remoteIp = extractIp(request);

        RefreshToken rt = new RefreshToken();
        rt.setJti(refresh.jti());
        rt.setUsername(user.getUsername());
        rt.setExpiresAt(expiresAt);
        rt.setIssuedAt(new Timestamp(System.currentTimeMillis()));
        rt.setUserAgent(Optional.ofNullable(request.getHeader("User-Agent")).orElse(""));
        rt.setIp(remoteIp);
        refreshTokenRepository.save(rt);

        setRefreshCookie(response, refresh.token());

        long expiresIn = jwtService.getAccessExpiresSeconds();
        return Map.of("access_token", accessToken, "token_type", "Bearer", "expires_in", expiresIn);
    }

    @Transactional
    public Map<String, String> refresh(HttpServletRequest request, HttpServletResponse response) {
        String cookieToken = extractRefreshCookie(request);
        if (cookieToken == null) {
            log.warn("Refresh attempt with no refresh token cookie");
            throw new UnauthorizedException("Missing refresh token");
        }

        Claims payload;
        try {
            payload = jwtService.verifyRefreshToken(cookieToken);
        } catch (Exception e) {
            log.warn("Refresh attempt with invalid or tampered token");
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String jti = payload.getId();
        String username = payload.get("username", String.class);

        RefreshToken stored = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in DB - possible replay for username: {}", username);
                    return new UnauthorizedException("Refresh token invalid/expired");
                });

        if (stored.getExpiresAt().getTime() <= System.currentTimeMillis()) {
            log.warn("Expired refresh token used for username: {}", username);
            throw new UnauthorizedException("Refresh token invalid/expired");
        }

        refreshTokenRepository.deleteById(jti);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String accessToken = jwtService.generateAccessToken(username, user.getUserrole());
        JwtService.RefreshTokenResult newRefresh = jwtService.generateRefreshToken(username);

        RefreshToken newRt = new RefreshToken();
        newRt.setJti(newRefresh.jti());
        newRt.setUsername(username);
        newRt.setExpiresAt(new Timestamp(System.currentTimeMillis() + REFRESH_MAX_AGE_MS));
        newRt.setIssuedAt(new Timestamp(System.currentTimeMillis()));
        newRt.setUserAgent(Optional.ofNullable(request.getHeader("User-Agent")).orElse(""));
        newRt.setIp(extractIp(request));
        refreshTokenRepository.save(newRt);

        setRefreshCookie(response, newRefresh.token());
        return Map.of("token", accessToken);
    }

    @Transactional
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response,
                                       String contextUsername) {
        String cookieToken = extractRefreshCookie(request);
        if (cookieToken != null) {
            try {
                Claims payload = jwtService.verifyRefreshToken(cookieToken);
                refreshTokenRepository.deleteById(payload.getId());
            } catch (Exception e) {
                Claims decoded = jwtService.decodeWithoutVerify(cookieToken);
                if (decoded != null && decoded.getId() != null) {
                    refreshTokenRepository.deleteById(decoded.getId());
                }
            }
        }
        if (contextUsername != null) {
            refreshTokenRepository.deleteAllByUsername(contextUsername);
        }

        clearRefreshCookie(response);
        return Map.of("message", "Logged out");
    }

    @Transactional
    public Map<String, String> forgotPassword(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String resetToken = jwtService.generateResetToken(username);
        String resetLink = appProperties.getProdUrl() +
                "/stubserver/resetPassword?token=" + java.net.URLEncoder.encode(resetToken, java.nio.charset.StandardCharsets.UTF_8);

        try {
            mailService.sendEmail(
                    user.getEmail(),
                    "StubServer Account Password Reset",
                    "templates/resetPasswordEmailTemplate.html",
                    Map.of("firstname", nvl(user.getFirstname()), "lastname", nvl(user.getLastname()),
                            "resetLink", resetLink)
            );
        } catch (Exception e) {
            log.error("Email sending failed: {}", e.getMessage());
        }

        return Map.of("message", "Reset email sent", "token", resetToken);
    }

    @Transactional
    public Map<String, String> resetPassword(String token, String newPassword) {
        if (tokenService.isResetTokenUsed(token)) {
            log.warn("Password reset token reuse attempt detected");
            throw new ForbiddenException("Token has already been used");
        }

        Claims payload;
        try {
            payload = jwtService.verifyResetToken(token);
        } catch (Exception e) {
            log.warn("Invalid or expired password reset token used");
            throw new BadRequestException("Invalid or expired token");
        }

        if (!"password_reset".equals(payload.get("purpose", String.class))) {
            throw new BadRequestException("Invalid token purpose");
        }

        String username = payload.get("username", String.class);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setPassword(tokenService.hashPassword(newPassword));
        userRepository.save(user);
        tokenService.markResetTokenUsed(token);
        return Map.of("message", "Password reset successfully");
    }

    @Transactional
    public Map<String, String> changePassword(String contextUsername, String tokenInBody,
                                               String currentPassword, String newPassword) {
        String username = contextUsername;
        if (tokenInBody != null && !tokenInBody.isEmpty()) {
            try {
                Claims p = jwtService.verifyAccessToken(tokenInBody);
                String u = p.get("username", String.class);
                if (u != null) username = u;
            } catch (Exception ignored) {}
        }

        if (username == null) throw new UnauthorizedException("Unauthorized");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!tokenService.verifyPassword(currentPassword, user.getPassword())) {
            log.warn("Change password failed - wrong current password for username: {}", username);
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(tokenService.hashPassword(newPassword));
        userRepository.save(user);
        return Map.of("message", "Password changed successfully");
    }

    // ===== Helpers =====

    private void setRefreshCookie(HttpServletResponse response, String token) {
        String cookieHeader = REFRESH_COOKIE + "=" + token +
                "; Path=/backend/api; HttpOnly; Secure; SameSite=Strict; Max-Age=" +
                (REFRESH_MAX_AGE_MS / 1000);
        response.addHeader("Set-Cookie", cookieHeader);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        String cookieHeader = REFRESH_COOKIE + "=; Path=/backend/api; HttpOnly; Secure; SameSite=Strict; Max-Age=0";
        response.addHeader("Set-Cookie", cookieHeader);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return Optional.ofNullable(request.getRemoteAddr()).orElse("");
    }

    public static record BasicAuth(String username, String password) {}

    public static BasicAuth extractBasicAuth(String header) {
        if (header == null || !header.startsWith("Basic ")) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()));
            int idx = decoded.indexOf(':');
            if (idx < 0) return null;
            String u = decoded.substring(0, idx);
            String p = decoded.substring(idx + 1);
            if (u.isEmpty() || p.isEmpty()) return null;
            return new BasicAuth(u, p);
        } catch (Exception e) {
            return null;
        }
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
