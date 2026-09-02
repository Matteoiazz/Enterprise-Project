package com.tripify.booking_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Come tutti gli altri test del modulo: senza questo profilo carica
// application.properties "vero" (Postgres reale + INTERNAL_SERVICE_KEY
// obbligatoria in ambiente), fallendo su qualunque macchina senza quelle
// due cose pronte - col profilo test usa H2 e i valori fittizi di
// application-test.properties, come il resto della suite.
@SpringBootTest
@ActiveProfiles("test")
class BookingServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
