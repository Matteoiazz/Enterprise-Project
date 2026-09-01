package com.tripify.api_gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final long WINDOW_MILLIS = 60_000;

    @Value("${gateway.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${gateway.rate-limit.max-per-minute:3000}")
    private int maxRequestsPerWindow;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(exchange);
        Bucket bucket = buckets.computeIfAbsent(clientIp, key -> new Bucket());

        if (!bucket.tryConsume(maxRequestsPerWindow)) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        evictStaleBucketsIfNeeded();
        return chain.filter(exchange);
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        String forwarded = headers.getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = headers.getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        return remote != null && remote.getAddress() != null
                ? remote.getAddress().getHostAddress()
                : "unknown";
    }

    private void evictStaleBucketsIfNeeded() {
        if (buckets.size() < 10_000) {
            return;
        }
        long cutoff = System.currentTimeMillis() - (2 * WINDOW_MILLIS);
        buckets.entrySet().removeIf(entry -> entry.getValue().windowStart < cutoff);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private static class Bucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        synchronized boolean tryConsume(int max) {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MILLIS) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= max;
        }
    }
}
