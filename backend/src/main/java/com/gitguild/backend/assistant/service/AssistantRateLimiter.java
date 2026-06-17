package com.gitguild.backend.assistant.service;

import com.gitguild.backend.assistant.config.AssistantRateLimitProperties;
import com.gitguild.backend.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AssistantRateLimiter {

    private static final int MAX_BUCKETS_BEFORE_CLEANUP = 4096;

    private final AssistantRateLimitProperties properties;
    private final Clock clock;
    private final Map<String, Deque<Long>> requestBuckets = new ConcurrentHashMap<>();

    @Autowired
    public AssistantRateLimiter(AssistantRateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AssistantRateLimiter(AssistantRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void check(AssistantChatContext context, HttpServletRequest request) {
        if (!properties.enabled()) {
            return;
        }

        long now = clock.millis();
        long windowStart = now - properties.windowSeconds() * 1000L;
        String key = rateLimitKey(context, request);
        Deque<Long> bucket = requestBuckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (bucket) {
            prune(bucket, windowStart);
            if (bucket.size() >= properties.maxRequests()) {
                throw new BusinessException(
                        "RATE_LIMIT_EXCEEDED",
                        HttpStatus.TOO_MANY_REQUESTS,
                        "对话过于频繁，请不要频繁对话，稍后再试",
                        "maxRequests=%d, windowSeconds=%d".formatted(
                                properties.maxRequests(),
                                properties.windowSeconds()));
            }
            bucket.addLast(now);
        }

        cleanupIfNeeded(windowStart);
    }

    private String rateLimitKey(AssistantChatContext context, HttpServletRequest request) {
        if (context.authenticated()) {
            return "user:" + context.userId();
        }
        return "ip:" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = firstHeaderValue(request.getHeader("X-Forwarded-For"));
        if (!forwardedFor.isBlank()) {
            return forwardedFor;
        }
        String realIp = firstHeaderValue(request.getHeader("X-Real-IP"));
        if (!realIp.isBlank()) {
            return realIp;
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr.trim();
    }

    private String firstHeaderValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int commaIndex = value.indexOf(',');
        return (commaIndex >= 0 ? value.substring(0, commaIndex) : value).trim();
    }

    private void prune(Deque<Long> bucket, long windowStart) {
        while (!bucket.isEmpty() && bucket.peekFirst() <= windowStart) {
            bucket.removeFirst();
        }
    }

    private void cleanupIfNeeded(long windowStart) {
        if (requestBuckets.size() < MAX_BUCKETS_BEFORE_CLEANUP) {
            return;
        }
        requestBuckets.entrySet().removeIf(entry -> {
            Deque<Long> bucket = entry.getValue();
            synchronized (bucket) {
                prune(bucket, windowStart);
                return bucket.isEmpty();
            }
        });
    }
}
