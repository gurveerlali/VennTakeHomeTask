package org.venn.takeHomeTest.transactions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.venn.takeHomeTest.repository.TransactionRepository;
import org.venn.takeHomeTest.repository.entity.Transaction;
import org.venn.takeHomeTest.transactions.dto.LoadRequest;
import org.venn.takeHomeTest.transactions.dto.LoadResponse;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionServiceTests {

    private TransactionRepository transactionRepository;
    private TransactionService transactionService;

    @BeforeEach
    void setup() {
        transactionRepository = mock(TransactionRepository.class);
        transactionService = new TransactionService(transactionRepository);

        // Default: no existing loads
        when(transactionRepository.existsByLoadIdAndCustomerId(anyString(), anyString())).thenReturn(false);
        when(transactionRepository.countAcceptedLoads(anyString(), any(), any())).thenReturn(0L);
        when(transactionRepository.sumAcceptedAmount(anyString(), any(), any())).thenReturn(BigDecimal.ZERO);
    }

    private LoadRequest buildRequest(String id, String customerId, String amount, String time) {
        LoadRequest request = new LoadRequest();
        request.setId(id);
        request.setCustomerId(customerId);
        request.setLoadAmount(amount);
        request.setTime(time);
        return request;
    }

    @Test
    void givenValidInput_whenPostLoad_thenAcceptsValidLoadAndReturnData() {
        LoadRequest request = buildRequest("1", "100", "$1000.00", "2000-01-03T10:00:00Z");

        LoadResponse response = transactionService.postLoad(request);

        assertNotNull(response);
        assertEquals("1", response.getId());
        assertEquals("100", response.getCustomerId());
        assertTrue(response.isAccepted());
    }

    @Test
    void givenValidInput_whenPostLoad_thenSavesTransaction() {
        LoadRequest request = buildRequest("1", "100", "$500.00", "2000-01-03T10:00:00Z");

        transactionService.postLoad(request);

        verify(transactionRepository).save(argThat(t ->
                t.getLoadId().equals("1")
                        && t.getCustomerId().equals("100")
                        && t.getAmount().compareTo(new BigDecimal("500.00")) == 0
                        && t.isAccepted()
        ));
    }

    @Test
    void givenDuplicateLoadId_whenPostLoad_thenIgnoresDuplicateLoadId() {
        when(transactionRepository.existsByLoadIdAndCustomerId("1", "100")).thenReturn(true);

        LoadRequest request = buildRequest("1", "100", "$500.00", "2000-01-03T10:00:00Z");
        LoadResponse response = transactionService.postLoad(request);

        assertNull(response);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void givenDuplicateLoadIdWithDifferentCustomerId_whenPostLoad_thenAcceptsLoad() {
        when(transactionRepository.existsByLoadIdAndCustomerId("1", "100")).thenReturn(true);
        when(transactionRepository.existsByLoadIdAndCustomerId("1", "200")).thenReturn(false);

        LoadRequest request = buildRequest("1", "200", "$500.00", "2000-01-03T10:00:00Z");
        LoadResponse response = transactionService.postLoad(request);

        assertNotNull(response);
        assertTrue(response.isAccepted());
    }


    @Test
    void givenTooManyDailyLoads_whenPostLoad_thenDeclinesLoad() {
        Instant dayStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-04T00:00:00Z");

        when(transactionRepository.countAcceptedLoads("100", dayStart, dayEnd)).thenReturn(3L);
        when(transactionRepository.sumAcceptedAmount("100", dayStart, dayEnd)).thenReturn(new BigDecimal("1000"));

        LoadRequest request = buildRequest("4", "100", "$100.00", "2000-01-03T15:00:00Z");
        LoadResponse response = transactionService.postLoad(request);

        assertNotNull(response);
        assertFalse(response.isAccepted());
    }

    @Test
    void givenDailyLimitExceeded_whenPostLoad_thenDeclinesLoad() {
        Instant dayStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-04T00:00:00Z");

        when(transactionRepository.countAcceptedLoads("100", dayStart, dayEnd)).thenReturn(1L);
        when(transactionRepository.sumAcceptedAmount("100", dayStart, dayEnd)).thenReturn(new BigDecimal("4000"));

        LoadRequest request = buildRequest("2", "100", "$1500.00", "2000-01-03T15:00:00Z");
        LoadResponse response = transactionService.postLoad(request);

        assertNotNull(response);
        assertFalse(response.isAccepted());
    }

    @Test
    void givenWeeklyLimitExceeded_whenPostLoad_thenDeclinesLoad() {
        Instant dayStart = Instant.parse("2000-01-05T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-06T00:00:00Z");
        Instant weekStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant weekEnd = Instant.parse("2000-01-10T00:00:00Z");

        when(transactionRepository.countAcceptedLoads("100", dayStart, dayEnd)).thenReturn(0L);
        when(transactionRepository.sumAcceptedAmount("100", dayStart, dayEnd)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumAcceptedAmount("100", weekStart, weekEnd)).thenReturn(new BigDecimal("18000"));

        LoadRequest request = buildRequest("10", "100", "$3000.00", "2000-01-05T10:00:00Z");
        LoadResponse response = transactionService.postLoad(request);

        assertNotNull(response);
        assertFalse(response.isAccepted());
    }

    @Test
    void givenDeniedTransaction_whenPostLoad_thenSavesTransaction() {
        Instant dayStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-04T00:00:00Z");

        when(transactionRepository.countAcceptedLoads("100", dayStart, dayEnd)).thenReturn(3L);

        LoadRequest request = buildRequest("4", "100", "$100.00", "2000-01-03T15:00:00Z");
        transactionService.postLoad(request);

        verify(transactionRepository).save(argThat(t ->
                t.getLoadId().equals("4") && !t.isAccepted()
        ));
    }
}
