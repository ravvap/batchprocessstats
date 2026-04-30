package gov.fdic.tip.bps.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Entra → Spring Security role-name mapping (BPS-003, BPS-009).
 */
class EntraRolesConverterTest {

    private final SecurityConfig.EntraRolesConverter converter =
            new SecurityConfig.EntraRolesConverter();

    // ------------------------------------------------------------------ //
    //  toSpringRole mapping                                               //
    // ------------------------------------------------------------------ //

    @ParameterizedTest(name = "Entra role ''{0}'' maps to ''{1}''")
    @CsvSource({
            "Admin,                ROLE_ADMIN",
            "Manager,              ROLE_MANAGER",
            "Sr. Analyst,          ROLE_SR_ANALYST",
            "Analyst,              ROLE_ANALYST",
            "BatchStatistics.Write,ROLE_BATCH_RUNNER"
    })
    @DisplayName("Entra role names are correctly mapped to Spring ROLE_ authorities")
    void toSpringRole_knownRoles_mappedCorrectly(String entraRole, String expectedRole) {
        String actual = SecurityConfig.EntraRolesConverter.toSpringRole(entraRole.trim());
        assertThat(actual).isEqualTo(expectedRole.trim());
    }

    // ------------------------------------------------------------------ //
    //  JWT roles claim conversion                                         //
    // ------------------------------------------------------------------ //

    @ParameterizedTest(name = "JWT with roles {0} produces authority {1}")
    @CsvSource({
            "Admin,                ROLE_ADMIN",
            "Manager,              ROLE_MANAGER",
            "Sr. Analyst,          ROLE_SR_ANALYST",
            "Analyst,              ROLE_ANALYST",
            "BatchStatistics.Write,ROLE_BATCH_RUNNER"
    })
    @DisplayName("converter extracts roles claim and produces correct GrantedAuthority")
    void convert_jwtWithRole_producesCorrectAuthority(String entraRole, String expectedAuthority) {
        Jwt jwt = buildJwt(List.of(entraRole.trim()));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains(expectedAuthority.trim());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("empty roles claim returns empty authority collection")
    void convert_emptyRoles_returnsEmptyCollection() {
        Jwt jwt = buildJwt(List.of());
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("null roles claim returns empty authority collection")
    void convert_nullRoles_returnsEmptyCollection() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user-001")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        assertThat(converter.convert(jwt)).isEmpty();
    }

    // ------------------------------------------------------------------ //
    //  Helper                                                             //
    // ------------------------------------------------------------------ //

    private Jwt buildJwt(List<String> roles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("sub", "user-001")
                .claim("roles", roles)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
