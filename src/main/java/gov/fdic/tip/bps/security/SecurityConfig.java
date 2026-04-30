package gov.fdic.tip.bps.security;

import gov.fdic.tip.bps.config.ApplicationConstants.ApiPaths;
import gov.fdic.tip.bps.config.ApplicationConstants.EntraRoles;
import gov.fdic.tip.bps.config.ApplicationConstants.JwtClaims;
import gov.fdic.tip.bps.config.ApplicationConstants.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * All string literals replaced with ApplicationConstants references.
 * JwtDecoder lives in JwtDecoderConfig to allow @WebMvcTest slices to
 * replace it with a @MockBean without triggering OIDC discovery.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
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
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new EntraRolesConverter());
        return converter;
    }

    // ------------------------------------------------------------------ //
    //  Inner converter: 'roles' claim -> Spring GrantedAuthority          //
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

        static String toSpringRole(String entraRole) {
            if (EntraRoles.ADMIN.equals(entraRole))                  return Roles.ADMIN;
            if (EntraRoles.MANAGER.equals(entraRole))                return Roles.MANAGER;
            if (EntraRoles.SR_ANALYST.equals(entraRole))             return Roles.SR_ANALYST;
            if (EntraRoles.ANALYST.equals(entraRole))                return Roles.ANALYST;
            if (EntraRoles.BATCH_STATISTICS_WRITE.equals(entraRole)) return Roles.BATCH_RUNNER;
            return "ROLE_" + entraRole.toUpperCase().replaceAll("[^A-Z0-9]", "_");
        }
    }
}
