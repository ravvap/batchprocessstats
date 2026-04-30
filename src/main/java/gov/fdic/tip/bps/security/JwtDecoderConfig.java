package gov.fdic.tip.bps.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

/**
 * Registers the real JwtDecoder bean separately from SecurityConfig.
 *
 * The @ConditionalOnMissingBean annotation is the critical piece:
 * when a test declares @MockBean JwtDecoder, Spring registers that mock
 * bean first, so this @Bean method is skipped entirely — preventing the
 * JwtDecoders.fromIssuerLocation() OIDC discovery HTTP call from firing
 * during @WebMvcTest slice context startup.
 */
@Configuration
public class JwtDecoderConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }
}
