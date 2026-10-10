package com.bank.simulator.security;

import com.bank.simulator.dto.ErrorResponse;
import com.bank.simulator.entity.AuditAction;
import com.bank.simulator.service.AuditLogService;
import com.bank.simulator.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiterService rateLimiterService, AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if ("/auth/login".equalsIgnoreCase(path) && "POST".equalsIgnoreCase(request.getMethod())) {
            String clientIp = getClientIp(request);
            if (!rateLimiterService.allowLoginRequest(clientIp)) {
                auditLogService.logEvent(clientIp, AuditAction.RATE_LIMIT_EXCEEDED, "ENDPOINT", "/auth/login", "Login rate limit exceeded", "REJECTED");
                sendRateLimitErrorResponse(response, "Too many login attempts. Please try again later.");
                return;
            }
        } else if ("/client/transfers".equalsIgnoreCase(path) && "POST".equalsIgnoreCase(request.getMethod())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String identity = (auth != null && auth.isAuthenticated()) ? auth.getName() : getClientIp(request);
            if (!rateLimiterService.allowTransferRequest(identity)) {
                auditLogService.logEvent(identity, AuditAction.RATE_LIMIT_EXCEEDED, "ENDPOINT", "/client/transfers", "Transfer rate limit exceeded", "REJECTED");
                sendRateLimitErrorResponse(response, "Too many transfer requests. Please try again later.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private void sendRateLimitErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("TOO_MANY_REQUESTS")
                .message(message)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
