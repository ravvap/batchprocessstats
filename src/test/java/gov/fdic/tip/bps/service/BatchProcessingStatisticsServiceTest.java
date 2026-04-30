package gov.fdic.tip.bps.service;

import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto.PagedResponse;
import gov.fdic.tip.bps.dto.BatchProcessingStatisticsDto.Response;
import gov.fdic.tip.bps.entity.BatchJobHistory;
import gov.fdic.tip.bps.entity.BatchSourceSystem;
import gov.fdic.tip.bps.exception.BatchStatisticsNotFoundException;
import gov.fdic.tip.bps.exception.ServerManagedFieldException;
import gov.fdic.tip.bps.exception.SourceSystemNotFoundException;
import gov.fdic.tip.bps.repository.BatchJobHistoryRepository;
import gov.fdic.tip.bps.repository.BatchSourceSystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchProcessingStatisticsServiceTest {

    @Mock BatchJobHistoryRepository   jobHistoryRepository;
    @Mock BatchSourceSystemRepository sourceSystemRepository;

    @InjectMocks BatchProcessingStatisticsService service;

    private BatchSourceSystem sampleSourceSystem;
    private BatchJobHistory   sampleEntity;

    @BeforeEach
    void setUp() {
        sampleSourceSystem = new BatchSourceSystem();
        sampleSourceSystem.setId(1L);
        sampleSourceSystem.setSourceName("SIMS");

        sampleEntity = new BatchJobHistory();
        sampleEntity.setId(100L);
        sampleEntity.setSourceSystem(sampleSourceSystem);
        sampleEntity.setJobId(42L);
        sampleEntity.setJobType("BATCH");
        sampleEntity.setRetryCount(0);
        sampleEntity.setJobStatus("SUCCESS");
        sampleEntity.setStartTime(Instant.parse("2024-01-01T00:00:00Z"));
        sampleEntity.setEndTime(Instant.parse("2024-01-01T01:00:00Z"));
        sampleEntity.setStatus("COMPLETED");
        sampleEntity.setRecordsGathered(1000);
        sampleEntity.setRecordsChanged(980);
        sampleEntity.setRecordsProcessedCurrentPeriod(980);
        sampleEntity.setRecordsProcessedPriorPeriod(0);
        sampleEntity.setRecordsUnpostable(20);
    }

    // ------------------------------------------------------------------ //
    //  list — BPS-004                                                     //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("list — BPS-004")
    class ListTests {

        @Test
        @DisplayName("returns paged response with default sort")
        void list_defaultSort_returnsPagedResponse() {
            Page<BatchJobHistory> page =
                    new PageImpl<>(List.of(sampleEntity), PageRequest.of(0, 25), 1);
            when(jobHistoryRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<Response> result =
                    service.list(0, 25, "startTime,desc", null, null, null, null, null);

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content().get(0).getSourceSystemName()).isEqualTo("SIMS");
        }

        @Test
        @DisplayName("clamps page size to max 100")
        void list_sizeAboveMax_clampedTo100() {
            Page<BatchJobHistory> page =
                    new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
            when(jobHistoryRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            service.list(0, 999, "startTime,desc", null, null, null, null, null);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(jobHistoryRepository)
                    .findAll(any(org.springframework.data.jpa.domain.Specification.class), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for unknown sort field")
        void list_unknownSortField_throws400() {
            assertThatThrownBy(() ->
                    service.list(0, 25, "unknownField,asc", null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown sort field");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for invalid sort direction")
        void list_invalidDirection_throws400() {
            assertThatThrownBy(() ->
                    service.list(0, 25, "startTime,sideways", null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  getById — BPS-005                                                  //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getById — BPS-005")
    class GetByIdTests {

        @Test
        @DisplayName("returns response when record exists")
        void getById_exists_returnsResponse() {
            when(jobHistoryRepository.findById(100L)).thenReturn(Optional.of(sampleEntity));

            Response result = service.getById(100L);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getJobStatus()).isEqualTo("SUCCESS");
            assertThat(result.getSourceSystemName()).isEqualTo("SIMS");
        }

        @Test
        @DisplayName("throws BatchStatisticsNotFoundException when not found")
        void getById_notFound_throws404() {
            when(jobHistoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BatchStatisticsNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  create — BPS-006                                                   //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("create — BPS-006")
    class CreateTests {

        @Test
        @DisplayName("saves and returns created record")
        void create_validBody_savesEntity() {
            when(sourceSystemRepository.findById(1L))
                    .thenReturn(Optional.of(sampleSourceSystem));
            when(jobHistoryRepository.save(any())).thenReturn(sampleEntity);

            Response result = service.create(validRequestBody());

            verify(jobHistoryRepository).save(any(BatchJobHistory.class));
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getSourceSystemName()).isEqualTo("SIMS");
        }

        @Test
        @DisplayName("throws SourceSystemNotFoundException when sourceSystemId not found")
        void create_invalidSourceSystem_throws404() {
            when(sourceSystemRepository.findById(99L)).thenReturn(Optional.empty());

            BatchProcessingStatisticsDto.RequestBody body = new BatchProcessingStatisticsDto.RequestBody(
                    99L, null, "BATCH", 0, "RUNNING",
                    Instant.now(), null, "ACTIVE", null,
                    0, 0, 0, 0, 0, null);

            assertThatThrownBy(() -> service.create(body))
                    .isInstanceOf(SourceSystemNotFoundException.class);
        }

        @Test
        @DisplayName("throws ServerManagedFieldException when id is present")
        void create_withIdField_throws400() {
            BatchProcessingStatisticsDto.RequestBody body = new BatchProcessingStatisticsDto.RequestBody(
                    1L, null, "BATCH", 0, "RUNNING",
                    Instant.now(), null, "ACTIVE", null,
                    0, 0, 0, 0, 0,
                    100L); // id supplied — forbidden

            assertThatThrownBy(() -> service.create(body))
                    .isInstanceOf(ServerManagedFieldException.class)
                    .satisfies(ex -> assertThat(
                            ((ServerManagedFieldException) ex).getOffendingFields())
                            .contains("id"));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when endTime < startTime")
        void create_endBeforeStart_throws400() {
            Instant start = Instant.parse("2024-06-01T10:00:00Z");
            Instant end   = Instant.parse("2024-06-01T09:00:00Z");

            // No sourceSystem stub needed — validation fires before the FK lookup
            BatchProcessingStatisticsDto.RequestBody body = new BatchProcessingStatisticsDto.RequestBody(
                    1L, null, "BATCH", 0, "RUNNING",
                    start, end, "ACTIVE", null,
                    0, 0, 0, 0, 0, null);

            assertThatThrownBy(() -> service.create(body))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("endTime");
        }
    }

    // ------------------------------------------------------------------ //
    //  replace — BPS-007                                                  //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("replace — BPS-007")
    class ReplaceTests {

        @Test
        @DisplayName("updates all fields and returns 200")
        void replace_existingRecord_updatesFields() {
            when(jobHistoryRepository.findById(100L)).thenReturn(Optional.of(sampleEntity));
            when(sourceSystemRepository.findById(1L)).thenReturn(Optional.of(sampleSourceSystem));
            when(jobHistoryRepository.save(any())).thenReturn(sampleEntity);

            Response result = service.replace(100L, validRequestBody());

            verify(jobHistoryRepository).save(any(BatchJobHistory.class));
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("throws BatchStatisticsNotFoundException when id not found")
        void replace_notFound_throws404() {
            when(jobHistoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.replace(999L, validRequestBody()))
                    .isInstanceOf(BatchStatisticsNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    private BatchProcessingStatisticsDto.RequestBody validRequestBody() {
        return new BatchProcessingStatisticsDto.RequestBody(
                1L,                                     // sourceSystemId
                42L,                                    // jobId
                "BATCH",                                // jobType
                0,                                      // retryCount
                "SUCCESS",                              // jobStatus
                Instant.parse("2024-01-01T00:00:00Z"), // startTime
                Instant.parse("2024-01-01T01:00:00Z"), // endTime
                "COMPLETED",                            // status
                null,                                   // errorMessage
                980,                                    // recordsChanged
                1000,                                   // recordsGathered
                980,                                    // recordsProcessedCurrentPeriod
                0,                                      // recordsProcessedPriorPeriod
                20,                                     // recordsUnpostable
                null                                    // id (server-managed)
        );
    }
}
