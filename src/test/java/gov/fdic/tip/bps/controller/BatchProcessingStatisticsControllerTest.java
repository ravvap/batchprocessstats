package gov.fdic.tip.bps.controller;

import gov.fdic.tip.bps.config.GlobalExceptionHandler;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto.Response;
import gov.fdic.tip.bps.entity.BatchType;
import gov.fdic.tip.bps.exception.BatchStatisticsNotFoundException;
import gov.fdic.tip.bps.security.SecurityConfig;
import gov.fdic.tip.bps.service.BatchProcessingStatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = BatchProcessingStatisticsController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
    }
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/test-tenant/v2.0",
    "spring.security.oauth2.resourceserver.jwt.audiences=api://test-audience"
})
class BatchProcessingStatisticsControllerTest {

    private static final String BASE_URL = "/api/v1/batch-processing-statistics";

    // Role constants matching ApplicationConstants
    private static final String ROLE_VIEW = "ROLE_BATCH_PRCS_STATS_VIEW";
    private static final String ROLE_ADD  = "ROLE_BATCH_PRCS_STATS_ADD";
    private static final String ROLE_EDIT = "ROLE_BATCH_PRCS_STATS_EDIT";

    @Autowired MockMvc mockMvc;

    @MockBean JwtDecoder jwtDecoder;
    @MockBean BatchProcessingStatisticsService service;

    // ------------------------------------------------------------------ //
    //  GET list — BPS-004                                                 //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("GET list — BPS-004")
    class GetListTests {

        @Test
        @DisplayName("200 for VIEW role")
        void list_viewRole_returns200() throws Exception {
            when(service.list(anyInt(), anyInt(), anyString(),
                    any(), any(), any(), any(), any()))
                    .thenReturn(emptyPage());

            mockMvc.perform(get(BASE_URL)
                            .with(jwt().authorities(() -> ROLE_VIEW)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("401 for unauthenticated request")
        void list_noAuth_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 for ADD role — no VIEW access")
        void list_addRole_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(jwt().authorities(() -> ROLE_ADD)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 for invalid sort field — problem+json body")
        void list_invalidSort_returns400() throws Exception {
            when(service.list(anyInt(), anyInt(), eq("badField,asc"),
                    any(), any(), any(), any(), any()))
                    .thenThrow(new IllegalArgumentException("Unknown sort field: badField"));

            mockMvc.perform(get(BASE_URL)
                            .param("sort", "badField,asc")
                            .with(jwt().authorities(() -> ROLE_VIEW)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }

        @Test
        @DisplayName("200 with sourceName filter")
        void list_withSourceNameFilter_returns200() throws Exception {
            when(service.list(anyInt(), anyInt(), anyString(),
                    eq("SIMS"), any(), any(), any(), any()))
                    .thenReturn(emptyPage());

            mockMvc.perform(get(BASE_URL)
                            .param("sourceName", "SIMS")
                            .with(jwt().authorities(() -> ROLE_VIEW)))
                    .andExpect(status().isOk());
        }
    }

    // ------------------------------------------------------------------ //
    //  GET by id — BPS-005                                                //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("GET by id — BPS-005")
    class GetByIdTests {

        @Test
        @DisplayName("200 with full record for VIEW role")
        void getById_found_returns200() throws Exception {
            when(service.getById(100L)).thenReturn(sampleResponse());

            mockMvc.perform(get(BASE_URL + "/100")
                            .with(jwt().authorities(() -> ROLE_VIEW)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.jobStatus").value("SUCCESS"))
                    .andExpect(jsonPath("$.sourceSystemName").value("SIMS"))
                    // Audit fields always present in response (BPS-010)
                    .andExpect(jsonPath("$.createdBy").value("svc-batch-runner"))
                    .andExpect(jsonPath("$.updatedBy").value("svc-batch-runner"))
                    .andExpect(jsonPath("$.createdDateTime").exists())
                    .andExpect(jsonPath("$.updatedDateTime").exists());
        }

        @Test
        @DisplayName("404 when record not found — problem+json body")
        void getById_notFound_returns404() throws Exception {
            when(service.getById(999L))
                    .thenThrow(new BatchStatisticsNotFoundException(999L));

            mockMvc.perform(get(BASE_URL + "/999")
                            .with(jwt().authorities(() -> ROLE_VIEW)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }

        @Test
        @DisplayName("403 for ADD role — no VIEW access")
        void getById_addRole_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/100")
                            .with(jwt().authorities(() -> ROLE_ADD)))
                    .andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------------------------------ //
    //  POST — BPS-006                                                     //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("POST — BPS-006")
    class PostTests {

        @Test
        @DisplayName("201 with Location header for ADD role")
        void post_validBody_returns201() throws Exception {
            when(service.create(any(BatchProcessingStatisticsDto.PostRequestBody.class), anyString()))
                    .thenReturn(sampleResponse());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPostBody())
                            .with(jwt().authorities(() -> ROLE_ADD)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").value(100));
        }

        @Test
        @DisplayName("201 with Location header for EDIT role")
        void post_editRole_returns201() throws Exception {
            when(service.create(any(BatchProcessingStatisticsDto.PostRequestBody.class), anyString()))
                    .thenReturn(sampleResponse());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPostBody())
                            .with(jwt().authorities(() -> ROLE_EDIT)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("403 for VIEW role — POST not allowed")
        void post_viewRole_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPostBody())
                            .with(jwt().authorities(() -> ROLE_VIEW)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 when required fields missing — errors array in problem+json")
        void post_missingRequiredFields_returns400() throws Exception {
            String body = """
                    { "startTime": "2024-01-01T00:00:00Z" }
                    """;
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(jwt().authorities(() -> ROLE_ADD)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("401 for unauthenticated POST")
        void post_noAuth_returns401() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPostBody()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------------------------ //
    //  PUT — BPS-007                                                      //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("PUT — BPS-007")
    class PutTests {

        @Test
        @DisplayName("200 for ADD role with valid body")
        void put_validBody_returns200() throws Exception {
            when(service.replace(eq(100L),
                    any(BatchProcessingStatisticsDto.PutRequestBody.class)))
                    .thenReturn(sampleResponse());

            mockMvc.perform(put(BASE_URL + "/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPutBody())
                            .with(jwt().authorities(() -> ROLE_ADD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100));
        }

        @Test
        @DisplayName("200 for EDIT role with valid body")
        void put_editRole_returns200() throws Exception {
            when(service.replace(eq(100L),
                    any(BatchProcessingStatisticsDto.PutRequestBody.class)))
                    .thenReturn(sampleResponse());

            mockMvc.perform(put(BASE_URL + "/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPutBody())
                            .with(jwt().authorities(() -> ROLE_EDIT)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("403 for VIEW role — PUT not allowed")
        void put_viewRole_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPutBody())
                            .with(jwt().authorities(() -> ROLE_VIEW)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("404 when record not found — problem+json body")
        void put_notFound_returns404() throws Exception {
            when(service.replace(eq(999L),
                    any(BatchProcessingStatisticsDto.PutRequestBody.class)))
                    .thenThrow(new BatchStatisticsNotFoundException(999L));

            mockMvc.perform(put(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPutBody())
                            .with(jwt().authorities(() -> ROLE_ADD)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }

        @Test
        @DisplayName("400 when required PUT fields are missing")
        void put_missingRequiredFields_returns400() throws Exception {
            String body = """
                    { "startTime": "2024-01-01T00:00:00Z" }
                    """;
            mockMvc.perform(put(BASE_URL + "/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(jwt().authorities(() -> ROLE_ADD)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isArray());
        }
    }

    // ------------------------------------------------------------------ //
    //  DELETE / PATCH — BPS-008 (405)                                    //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("DELETE / PATCH — BPS-008 (405)")
    class UnsupportedMethodTests {

        @Test
        @DisplayName("DELETE collection returns 405 with Allow header")
        void delete_collection_returns405() throws Exception {
            mockMvc.perform(delete(BASE_URL)
                            .with(jwt().authorities(() -> ROLE_VIEW)))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(header().exists("Allow"))
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }

        @Test
        @DisplayName("DELETE by id returns 405")
        void delete_byId_returns405() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/100")
                            .with(jwt().authorities(() -> ROLE_ADD)))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("PATCH by id returns 405")
        void patch_byId_returns405() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .with(jwt().authorities(() -> ROLE_ADD)))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Unauthenticated DELETE returns 401")
        void delete_unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/100"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    private BatchProcessingStatisticsDto.PagedResponse<Response> emptyPage() {
        return new BatchProcessingStatisticsDto.PagedResponse<>(
                List.of(), 0, 25, 0, 0, "startTime,desc");
    }

    private Response sampleResponse() {
        return Response.builder()
                .id(100L)
                // Audit fields — always returned (BPS-010)
                .createdBy("svc-batch-runner")
                .createdDateTime(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedBy("svc-batch-runner")
                .updatedDateTime(Instant.parse("2024-01-01T01:00:00Z"))
                // Source system
                .sourceSystemId(1L)
                .sourceSystemName("SIMS")
                // Job fields
                .jobId(42L)
                .jobType("BATCH")
                .batchType(BatchType.API)
                .retryCount(0)
                .jobStatus("SUCCESS")
                .startTime(Instant.parse("2024-01-01T00:00:00Z"))
                .endTime(Instant.parse("2024-01-01T01:00:00Z"))
                .status("COMPLETED")
                .recordsGathered(1000)
                .recordsChanged(980)
                .recordsProcessedCurrentPeriod(980)
                .recordsProcessedPriorPeriod(0)
                .recordsUnpostable(20)
                .build();
    }

    private String validPostBody() {
        return """
                {
                  "sourceSystemId": 1,
                  "jobId": 42,
                  "jobType": "BATCH",
                  "batchType": "API",
                  "retryCount": 0,
                  "jobStatus": "SUCCESS",
                  "startTime": "2024-01-01T00:00:00Z",
                  "endTime":   "2024-01-01T01:00:00Z",
                  "status": "COMPLETED",
                  "recordsGathered": 1000,
                  "recordsChanged":  980,
                  "recordsProcessedCurrentPeriod": 980,
                  "recordsProcessedPriorPeriod":     0,
                  "recordsUnpostable":              20
                }
                """;
    }

    private String validPutBody() {
        return """
                {
                  "processName":    "Nightly FDIC Import",
                  "startTime":      "2024-01-01T00:00:00Z",
                  "endTime":        "2024-01-01T01:00:00Z",
                  "type":           "BATCH",
                  "recordsGathered":  1000,
                  "recordsChanged":    980,
                  "errorRecords":       20,
                  "processedRecords":  960
                }
                """;
    }
}
