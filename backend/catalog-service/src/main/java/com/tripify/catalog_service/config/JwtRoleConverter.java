package com.tripify.catalog_service.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// I ruoli realm di Keycloak arrivano nel token come oggetto annidato
// ("realm_access": {"roles": [...]}), non come lista piatta: il converter di
// default di Spring Security non sa estrarli da soli. Letti direttamente dal
// JWT già validato, non da un header che il gateway dovrebbe (ma non sempre
// riesce a) propagare.
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @SuppressWarnings("unchecked")
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.toString()))
                .collect(Collectors.toList());
    }
}
