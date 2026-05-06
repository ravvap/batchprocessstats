package gov.fdic.tip.bps.security;

import gov.fdic.tip.bps.config.ApplicationConstants.ApiPaths;
import gov.fdic.tip.bps.config.ApplicationConstants.EntraRoles;
import gov.fdic.tip.bps.config.ApplicationConstants.JwtClaims;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security configuration (BPS-003, BPS-009).
 *
 * Two profiles:
 *
 *   default / prod  — JWT-secured; roles from Entra 'roles' claim are mapped to
 *                     ROLE_BATCH_PRCS_STATS_VIEW / ADD / EDIT via EntraRolesConverter.
 *
 *   local           — permits all requests without authentication so the API
 *                     can be exercised directly with curl / Postman without a token.
 *                     NEVER deploy this profile to a shared environment.
 *
 * JwtDecoder lives in JwtDecoderConfig to allow @WebMvcTest slices to replace
 * it with a @MockBean without triggering an OIDC discovery HTTP call.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // ------------------------------------------------------------------ //
    //  LOCAL profile — permit all (no auth required)                     //
    // ------------------------------------------------------------------ //

    @Bean
    @Profile("local")
    public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // ------------------------------------------------------------------ //
    //  DEFAULT / PROD profile — JWT + RBAC                               //
    // ------------------------------------------------------------------ //

    @Bean
    @Profile("!local")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    ApiPaths.OPENAPI_ALL,
                    ApiPaths.SWAGGER_UI_ALL,
                    ApiPaths.SWAGGER_UI)
                    .permitAll()
                .requestMatchers(ApiPaths.BPS_V1_BASE + "/**")
                    .authenticated()
                .anyRequest()
                    .authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        return http.build();
    }

    @Bean
    @Profile("!local")
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new EntraRolesConverter());
        return converter;
    }

    // ------------------------------------------------------------------ //
    //  Entra roles claim → Spring Security GrantedAuthority              //
    //                                                                    //
    //  Entra app roles (in JWT 'roles' claim):                           //
    //    BATCH_PRCS_STATS_VIEW  → ROLE_BATCH_PRCS_STATS_VIEW  (GET)     //
    //    BATCH_PRCS_STATS_ADD   → ROLE_BATCH_PRCS_STATS_ADD   (POST)    //
    //    BATCH_PRCS_STATS_EDIT  → ROLE_BATCH_PRCS_STATS_EDIT  (PUT)     //
    // ------------------------------------------------------------------ //

    static class EntraRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            List<String> roles = jwt.getClaimAsStringList(JwtClaims.ROLES);
            if (roles == null || roles.isEmpty()) return Collections.emptyList();
            return roles.stream()
                    .map(EntraRolesConverter::toSpringRole)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        /**
         * Entra role name → Spring ROLE_ string.
         * Role names match exactly — no transformation needed beyond prefixing.
         */
        static String toSpringRole(String entraRole) {
            return switch (entraRole) {
                case EntraRoles.BATCH_PRCS_STATS_VIEW -> "ROLE_" + EntraRoles.BATCH_PRCS_STATS_VIEW;
                case EntraRoles.BATCH_PRCS_STATS_ADD  -> "ROLE_" + EntraRoles.BATCH_PRCS_STATS_ADD;
                case EntraRoles.BATCH_PRCS_STATS_EDIT -> "ROLE_" + EntraRoles.BATCH_PRCS_STATS_EDIT;
                default -> "ROLE_" + entraRole.toUpperCase().replaceAll("[^A-Z0-9_]", "_");
            };
        }
    }
}
