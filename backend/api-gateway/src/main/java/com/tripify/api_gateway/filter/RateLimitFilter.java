package com.tripify.api_gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_CLIENTS = 50_000;

    @Value("${gateway.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${gateway.rate-limit.max-per-minute:3000}")
    private int maxRequestsPerWindow;

    @Value("${gateway.rate-limit.global-max-per-minute:20000}")
    private int globalMaxRequestsPerWindow;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Bucket globalBucket = new Bucket();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        if (globalMaxRequestsPerWindow > 0 && !globalBucket.tryConsume(globalMaxRequestsPerWindow)) {
            return reject(exchange);
        }

        String clientIp = resolveClientIp(exchange);

        if (!buckets.containsKey(clientIp) && buckets.size() >= MAX_TRACKED_CLIENTS) {
            purgeStaleBuckets();
            if (buckets.size() >= MAX_TRACKED_CLIENTS) {
                return reject(exchange);
            }
        }

        Bucket bucket = buckets.computeIfAbsent(clientIp, key -> new Bucket());
        if (!bucket.tryConsume(maxRequestsPerWindow)) {
            return reject(exchange);
        }

        return chain.filter(exchange);
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        InetSocketAddress remote = request.getRemoteAddress();
        InetAddress peer = remote != null ? remote.getAddress() : null;

        if (peer != null && isTrustedProxy(peer)) {
            HttpHeaders headers = request.getHeaders();

            String forwarded = headers.getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] hops = forwarded.split(",");
                String candidate = hops[hops.length - 1].trim();
                if (!candidate.isEmpty()) {
                    return candidate;
                }
            }

            String realIp = headers.getFirst("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }

        return peer != null ? peer.getHostAddress() : "unknown";
    }

    private boolean isTrustedProxy(InetAddress address) {
        return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress();
    }

    private void purgeStaleBuckets() {
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
            if (count.get() >= max) {
                return false;
            }
            count.incrementAndGet();
            return true;
        }
    }
}
