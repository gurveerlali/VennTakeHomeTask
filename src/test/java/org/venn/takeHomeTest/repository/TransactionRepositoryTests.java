package org.venn.takeHomeTest.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.venn.takeHomeTest.repository.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TransactionRepositoryTests {

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void cleanup() {
        transactionRepository.deleteAll();
    }

    private Transaction buildTransaction(String loadId, String customerId, BigDecimal amount,
                                          Instant time, boolean accepted) {
        Transaction t = new Transaction();
        t.setLoadId(loadId);
        t.setCustomerId(customerId);
        t.setAmount(amount);
        t.setTime(time);
        t.setAccepted(accepted);
        return t;
    }

    @Test
    void givenExistingTransaction_whenExistsByLoadIdAndCustomerId_thenReturnsTrue() {
        transactionRepository.save(buildTransaction("1", "100", new BigDecimal("500"), Instant.parse("2000-01-03T10:00:00Z"), true));

        assertTrue(transactionRepository.existsByLoadIdAndCustomerId("1", "100"));
    }

    @Test
    void givenNoTransactions_whenExistsByLoadIdAndCustomerId_thenReturnsFalse() {
        assertFalse(transactionRepository.existsByLoadIdAndCustomerId("1", "100"));
    }

    @Test
    void givenExistingTransaction_whenExistsByLoadIdAndCustomerId_thenMatchesBothFields() {
        transactionRepository.save(buildTransaction("1", "100", new BigDecimal("500"), Instant.parse("2000-01-03T10:00:00Z"), true));

        assertTrue(transactionRepository.existsByLoadIdAndCustomerId("1", "100"));
        assertFalse(transactionRepository.existsByLoadIdAndCustomerId("1", "200"));
        assertFalse(transactionRepository.existsByLoadIdAndCustomerId("2", "100"));
    }

    @Test
    void givenDeclinedTransaction_whenExistsByLoadIdAndCustomerId_thenReturnsTrue() {
        transactionRepository.save(buildTransaction("1", "100", new BigDecimal("500"), Instant.parse("2000-01-03T10:00:00Z"), false));

        assertTrue(transactionRepository.existsByLoadIdAndCustomerId("1", "100"));
    }


    @Test
    void givenMixedTransactions_whenSumAcceptedAmount_thenSumsOnlyAccepted() {
        Instant dayStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-04T00:00:00Z");

        transactionRepository.save(buildTransaction("1", "100", new BigDecimal("1000"), Instant.parse("2000-01-03T08:00:00Z"), true));
        transactionRepository.save(buildTransaction("2", "100", new BigDecimal("2000"), Instant.parse("2000-01-03T12:00:00Z"), true));
        transactionRepository.save(buildTransaction("3", "100", new BigDecimal("500"), Instant.parse("2000-01-03T16:00:00Z"), false));

        BigDecimal sum = transactionRepository.sumAcceptedAmount("100", dayStart, dayEnd);

        assertEquals(0, new BigDecimal("3000").compareTo(sum));
    }

    @Test
    void givenMultipleCustomers_whenSumAcceptedAmount_thenExcludesOtherCustomers() {
        Instant dayStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-04T00:00:00Z");

        transactionRepository.save(buildTransaction("1", "100", new BigDecimal("1000"), Instant.parse("2000-01-03T08:00:00Z"), true));
        transactionRepository.save(buildTransaction("2", "200", new BigDecimal("2000"), Instant.parse("2000-01-03T12:00:00Z"), true));

        BigDecimal sum = transactionRepository.sumAcceptedAmount("100", dayStart, dayEnd);

        assertEquals(0, new BigDecimal("1000").compareTo(sum));
    }

    @Test
    void givenTransactionsAcrossMultipleDays_whenSumAcceptedAmount_thenExcludesOutOfRange() {
        Instant dayStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-04T00:00:00Z");

        transactionRepository.save(buildTransaction("1", "100", new BigDecimal("1000"), Instant.parse("2000-01-03T10:00:00Z"), true));
        transactionRepository.save(buildTransaction("2", "100", new BigDecimal("2000"), Instant.parse("2000-01-02T10:00:00Z"), true));
        transactionRepository.save(buildTransaction("3", "100", new BigDecimal("3000"), Instant.parse("2000-01-04T00:00:00Z"), true));

        BigDecimal sum = transactionRepository.sumAcceptedAmount("100", dayStart, dayEnd);

        assertEquals(0, new BigDecimal("1000").compareTo(sum));
    }

    @Test
    void givenNoTransactions_whenSumAcceptedAmount_thenReturnsZero() {
        Instant dayStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-04T00:00:00Z");

        BigDecimal sum = transactionRepository.sumAcceptedAmount("100", dayStart, dayEnd);

        assertEquals(0, BigDecimal.ZERO.compareTo(sum));
    }


    @Test
    void givenMixedTransactions_whenCountAcceptedLoads_thenCountsOnlyAccepted() {
        Instant dayStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-04T00:00:00Z");

        transactionRepository.save(buildTransaction("1", "100", new BigDecimal("1000"), Instant.parse("2000-01-03T08:00:00Z"), true));
        transactionRepository.save(buildTransaction("2", "100", new BigDecimal("2000"), Instant.parse("2000-01-03T12:00:00Z"), true));
        transactionRepository.save(buildTransaction("3", "100", new BigDecimal("500"), Instant.parse("2000-01-03T16:00:00Z"), false));

        long count = transactionRepository.countAcceptedLoads("100", dayStart, dayEnd);

        assertEquals(2, count);
    }

    @Test
    void givenMultipleCustomers_whenCountAcceptedLoads_thenExcludesOtherCustomers() {
        Instant dayStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-04T00:00:00Z");

        transactionRepository.save(buildTransaction("1", "100", new BigDecimal("1000"), Instant.parse("2000-01-03T08:00:00Z"), true));
        transactionRepository.save(buildTransaction("2", "200", new BigDecimal("2000"), Instant.parse("2000-01-03T12:00:00Z"), true));

        long count = transactionRepository.countAcceptedLoads("100", dayStart, dayEnd);

        assertEquals(1, count);
    }

    @Test
    void givenTransactionsAcrossMultipleDays_whenCountAcceptedLoads_thenExcludesOutOfRange() {
        Instant dayStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-04T00:00:00Z");

        transactionRepository.save(buildTransaction("1", "100", new BigDecimal("1000"), Instant.parse("2000-01-03T10:00:00Z"), true));
        transactionRepository.save(buildTransaction("2", "100", new BigDecimal("2000"), Instant.parse("2000-01-02T23:59:59Z"), true));
        transactionRepository.save(buildTransaction("3", "100", new BigDecimal("3000"), Instant.parse("2000-01-04T00:00:00Z"), true));

        long count = transactionRepository.countAcceptedLoads("100", dayStart, dayEnd);

        assertEquals(1, count);
    }

    @Test
    void givenNoTransactions_whenCountAcceptedLoads_thenReturnsZero() {
        Instant dayStart = Instant.parse("2000-01-03T00:00:00Z");
        Instant dayEnd = Instant.parse("2000-01-04T00:00:00Z");

        long count = transactionRepository.countAcceptedLoads("100", dayStart, dayEnd);

        assertEquals(0, count);
    }
}
