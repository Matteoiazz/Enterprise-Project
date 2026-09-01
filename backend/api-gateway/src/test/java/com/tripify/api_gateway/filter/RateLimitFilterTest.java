package com.tripify.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private AtomicInteger chainCalls;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "maxRequestsPerWindow", 2);
        chainCalls = new AtomicInteger();
        chain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.empty();
        };
    }

    private MockServerWebExchange exchangeFrom(String ip) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/catalog/items/search").header("X-Forwarded-For", ip));
    }

    @Test
    void allowsRequestsUpToTheLimit_thenReturns429() {
        MockServerWebExchange first = exchangeFrom("10.0.0.1");
        MockServerWebExchange second = exchangeFrom("10.0.0.1");
        MockServerWebExchange third = exchangeFrom("10.0.0.1");

        filter.filter(first, chain).block();
        filter.filter(second, chain).block();
        filter.filter(third, chain).block();

        assertThat(chainCalls.get()).isEqualTo(2);
        assertThat(third.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void whenDisabled_alwaysForwards() {
        ReflectionTestUtils.setField(filter, "enabled", false);

        for (int i = 0; i < 10; i++) {
            filter.filter(exchangeFrom("10.0.0.9"), chain).block();
        }

        assertThat(chainCalls.get()).isEqualTo(10);
    }

    @Test
    void differentForwardedForValues_getSeparateBuckets() {
        filter.filter(exchangeFrom("1.1.1.1"), chain).block();
        filter.filter(exchangeFrom("1.1.1.1"), chain).block();
        filter.filter(exchangeFrom("2.2.2.2"), chain).block();
        filter.filter(exchangeFrom("2.2.2.2"), chain).block();

        assertThat(chainCalls.get()).isEqualTo(4);
    }

    @Test
    void usesFirstIpOfForwardedForChain() {
        MockServerWebExchange a = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header("X-Forwarded-For", "9.9.9.9, 10.0.0.1"));
        MockServerWebExchange b = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header("X-Forwarded-For", "9.9.9.9, 172.16.0.1"));
        MockServerWebExchange c = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header("X-Forwarded-For", "9.9.9.9"));

        filter.filter(a, chain).block();
        filter.filter(b, chain).block();
        filter.filter(c, chain).block();

        assertThat(chainCalls.get()).isEqualTo(2);
        assertThat(c.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void fallsBackToRealIpHeaderWhenNoForwardedFor() {
        ServerWebExchange a = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header("X-Real-IP", "8.8.8.8"));
        ServerWebExchange b = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header("X-Real-IP", "8.8.8.8"));
        MockServerWebExchange c = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header("X-Real-IP", "8.8.8.8"));

        filter.filter(a, chain).block();
        filter.filter(b, chain).block();
        filter.filter(c, chain).block();

        assertThat(chainCalls.get()).isEqualTo(2);
        assertThat(c.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void order_isMinusOne() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }
}
