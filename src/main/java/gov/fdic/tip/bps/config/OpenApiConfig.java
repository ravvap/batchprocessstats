package gov.fdic.tip.bps.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.1 specification configuration (BPS-010).
 *
 * Published at /v3/api-docs and rendered via Swagger UI at /swagger-ui.html.
 * Declares four operations: GET (list), GET (by id), POST, PUT.
 * DELETE and PATCH are NOT declared; the 405 response schema is attached via
 * the @ControllerAdvice handler (BPS-008) so clients can still find the shape.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI batchProcessingStatisticsOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Batch Processing Statistics API")
                        .description("""
                                TIP Platform — Batch Processing Statistics module.
                                Provides paginated read access for authorized human roles
                                and write access for the Batch Runner service principal.
                                """)
                        .version("v3.15"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Microsoft Entra ID issued JWT Bearer token.")));
    }
}
