package gov.fdic.tip.bps.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Entra roles claim → Spring Security GrantedAuthority mapping.
 *
 * New role mapping (BPS-003, BPS-009):
 *   BATCH_PRCS_STATS_VIEW  → ROLE_BATCH_PRCS_STATS_VIEW  (GET list + GET by id)
 *   BATCH_PRCS_STATS_ADD   → ROLE_BATCH_PRCS_STATS_ADD   (POST)
 *   BATCH_PRCS_STATS_EDIT  → ROLE_BATCH_PRCS_STATS_EDIT  (PUT)
 */
class EntraRolesConverterTest {

    private final SecurityConfig.EntraRolesConverter converter =
            new SecurityConfig.EntraRolesConverter();

    @ParameterizedTest(name = "Entra role ''{0}'' → Spring authority ''{1}''")
    @CsvSource({
            "BATCH_PRCS_STATS_VIEW, ROLE_BATCH_PRCS_STATS_VIEW",
            "BATCH_PRCS_STATS_ADD,  ROLE_BATCH_PRCS_STATS_ADD",
            "BATCH_PRCS_STATS_EDIT, ROLE_BATCH_PRCS_STATS_EDIT"
    })
    @DisplayName("Entra role names are correctly prefixed with ROLE_")
    void toSpringRole_knownRoles_mappedCorrectly(String entraRole, String expectedRole) {
        String actual = SecurityConfig.EntraRolesConverter.toSpringRole(entraRole.trim());
        assertThat(actual).isEqualTo(expectedRole.trim());
    }

    @ParameterizedTest(name = "JWT with role ''{0}'' produces authority ''{1}''")
    @CsvSource({
            "BATCH_PRCS_STATS_VIEW, ROLE_BATCH_PRCS_STATS_VIEW",
            "BATCH_PRCS_STATS_ADD,  ROLE_BATCH_PRCS_STATS_ADD",
            "BATCH_PRCS_STATS_EDIT, ROLE_BATCH_PRCS_STATS_EDIT"
    })
    @DisplayName("Converter extracts roles claim and produces correct GrantedAuthority")
    void convert_jwtWithRole_producesCorrectAuthority(String entraRole, String expectedAuthority) {
        Jwt jwt = buildJwt(List.of(entraRole.trim()));
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains(expectedAuthority.trim());
    }

    @Test
    @DisplayName("JWT with multiple roles produces all corresponding authorities")
    void convert_jwtWithMultipleRoles_producesAllAuthorities() {
        Jwt jwt = buildJwt(List.of(
                "BATCH_PRCS_STATS_VIEW",
                "BATCH_PRCS_STATS_ADD",
                "BATCH_PRCS_STATS_EDIT"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        "ROLE_BATCH_PRCS_STATS_VIEW",
                        "ROLE_BATCH_PRCS_STATS_ADD",
                        "ROLE_BATCH_PRCS_STATS_EDIT");
    }

    @Test
    @DisplayName("Empty roles claim returns empty authority collection")
    void convert_emptyRoles_returnsEmptyCollection() {
        Jwt jwt = buildJwt(List.of());
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    @DisplayName("Null roles claim returns empty authority collection")
    void convert_nullRoles_returnsEmptyCollection() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "svc-batch-runner")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        assertThat(converter.convert(jwt)).isEmpty();
    }

    private Jwt buildJwt(List<String> roles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("sub", "test-principal")
                .claim("roles", roles)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
