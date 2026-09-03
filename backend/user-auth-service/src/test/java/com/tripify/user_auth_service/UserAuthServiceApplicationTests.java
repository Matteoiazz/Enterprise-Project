package com.tripify.user_auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class UserAuthServiceApplicationTests {

	// Sostituisce il JwtDecoder reale: senza, all'avvio del contesto Spring
	// contatterebbe Keycloak (issuer-uri) e il test fallirebbe fuori da un
	// ambiente con l'infrastruttura su.
	@MockitoBean
	JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
	}

}
