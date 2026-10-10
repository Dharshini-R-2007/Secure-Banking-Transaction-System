package com.bank.simulator.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private static class RequestWindow {
        long windowStart;
        AtomicInteger count;

        RequestWindow(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RequestWindow> loginRateLimitMap = new ConcurrentHashMap<>();
    private final Map<String, RequestWindow> transferRateLimitMap = new ConcurrentHashMap<>();

    private static final int MAX_LOGIN_REQUESTS_PER_MINUTE = 10;
    private static final int MAX_TRANSFER_REQUESTS_PER_MINUTE = 20;
    private static final long WINDOW_DURATION_MS = 60_000L;

    public boolean allowLoginRequest(String clientKey) {
        return checkRateLimit(loginRateLimitMap, clientKey, MAX_LOGIN_REQUESTS_PER_MINUTE);
    }

    public boolean allowTransferRequest(String clientKey) {
        return checkRateLimit(transferRateLimitMap, clientKey, MAX_TRANSFER_REQUESTS_PER_MINUTE);
    }

    private boolean checkRateLimit(Map<String, RequestWindow> limitMap, String key, int maxAllowed) {
        long currentTime = System.currentTimeMillis();
        String safeKey = key != null ? key : "anonymous";

        RequestWindow window = limitMap.get(safeKey);

        if (window == null || (currentTime - window.windowStart) > WINDOW_DURATION_MS) {
            limitMap.put(safeKey, new RequestWindow(currentTime));
            return true;
        }

        if (window.count.get() >= maxAllowed) {
            return false;
        }

        window.count.incrementAndGet();
        return true;
    }

    public void reset() {
        loginRateLimitMap.clear();
        transferRateLimitMap.clear();
    }
}
