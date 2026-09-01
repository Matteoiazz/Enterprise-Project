package com.tripify.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static final InetSocketAddress TRUSTED_PROXY = new InetSocketAddress("127.0.0.1", 51000);
    private static final InetSocketAddress DIRECT_PUBLIC_PEER = new InetSocketAddress("203.0.113.7", 51000);

    private RateLimitFilter filter;
    private AtomicInteger chainCalls;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "maxRequestsPerWindow", 2);
        ReflectionTestUtils.setField(filter, "globalMaxRequestsPerWindow", 0);
        chainCalls = new AtomicInteger();
        chain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.empty();
        };
    }

    private MockServerWebExchange behindProxy(String forwardedFor) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/catalog/items/search")
                        .remoteAddress(TRUSTED_PROXY)
                        .header("X-Forwarded-For", forwardedFor));
    }

    @Test
    void allowsRequestsUpToTheLimitThenReturns429() {
        filter.filter(behindProxy("10.0.0.1"), chain).block();
        filter.filter(behindProxy("10.0.0.1"), chain).block();
        MockServerWebExchange third = behindProxy("10.0.0.1");
        filter.filter(third, chain).block();

        assertThat(chainCalls.get()).isEqualTo(2);
        assertThat(third.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void whenDisabledAlwaysForwards() {
        ReflectionTestUtils.setField(filter, "enabled", false);

        for (int i = 0; i < 10; i++) {
            filter.filter(behindProxy("10.0.0.9"), chain).block();
        }

        assertThat(chainCalls.get()).isEqualTo(10);
    }

    @Test
    void differentClientsGetSeparateBuckets() {
        filter.filter(behindProxy("1.1.1.1"), chain).block();
        filter.filter(behindProxy("1.1.1.1"), chain).block();
        filter.filter(behindProxy("2.2.2.2"), chain).block();
        filter.filter(behindProxy("2.2.2.2"), chain).block();

        assertThat(chainCalls.get()).isEqualTo(4);
    }

    @Test
    void spoofedLeftmostForwardedForEntriesAreIgnoredAndRightmostIsUsed() {
        filter.filter(behindProxy("1.2.3.4, 9.9.9.9"), chain).block();
        filter.filter(behindProxy("5.6.7.8, 9.9.9.9"), chain).block();
        MockServerWebExchange third = behindProxy("240.0.0.1, 9.9.9.9");
        filter.filter(third, chain).block();

        assertThat(chainCalls.get()).isEqualTo(2);
        assertThat(third.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void forwardedForFromUntrustedDirectPeerIsIgnored() {
        MockServerWebExchange a = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").remoteAddress(DIRECT_PUBLIC_PEER)
                        .header("X-Forwarded-For", "11.11.11.11"));
        MockServerWebExchange b = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").remoteAddress(DIRECT_PUBLIC_PEER)
                        .header("X-Forwarded-For", "22.22.22.22"));
        MockServerWebExchange c = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").remoteAddress(DIRECT_PUBLIC_PEER)
                        .header("X-Forwarded-For", "33.33.33.33"));

        filter.filter(a, chain).block();
        filter.filter(b, chain).block();
        filter.filter(c, chain).block();

        assertThat(chainCalls.get()).isEqualTo(2);
        assertThat(c.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void fallsBackToRealIpHeaderFromTrustedProxyWhenNoForwardedFor() {
        MockServerWebExchange a = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").remoteAddress(TRUSTED_PROXY).header("X-Real-IP", "8.8.8.8"));
        MockServerWebExchange b = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").remoteAddress(TRUSTED_PROXY).header("X-Real-IP", "8.8.8.8"));
        MockServerWebExchange c = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").remoteAddress(TRUSTED_PROXY).header("X-Real-IP", "8.8.8.8"));

        filter.filter(a, chain).block();
        filter.filter(b, chain).block();
        filter.filter(c, chain).block();

        assertThat(chainCalls.get()).isEqualTo(2);
        assertThat(c.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void globalBackstopCapsTotalThroughputEvenWithUniqueClientIdentities() {
        ReflectionTestUtils.setField(filter, "globalMaxRequestsPerWindow", 3);

        filter.filter(behindProxy("100.0.0.1"), chain).block();
        filter.filter(behindProxy("100.0.0.2"), chain).block();
        filter.filter(behindProxy("100.0.0.3"), chain).block();
        MockServerWebExchange overflow = behindProxy("100.0.0.4");
        filter.filter(overflow, chain).block();

        assertThat(chainCalls.get()).isEqualTo(3);
        assertThat(overflow.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void orderIsMinusOne() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }
}
