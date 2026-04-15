package org.venn.takeHomeTest.transactions;

import lombok.extern.slf4j.Slf4j;
import org.venn.takeHomeTest.transactions.dto.LoadRequest;
import org.venn.takeHomeTest.transactions.dto.LoadResponse;
import org.venn.takeHomeTest.repository.TransactionRepository;
import org.venn.takeHomeTest.repository.entity.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

@Slf4j
@Service
public class TransactionService {

    private static final BigDecimal DAILY_AMOUNT_LIMIT = new BigDecimal("5000");
    private static final BigDecimal WEEKLY_AMOUNT_LIMIT = new BigDecimal("20000");
    private static final long DAILY_LOAD_LIMIT = 3;

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    @Transactional
    public LoadResponse postLoad(LoadRequest request) {
        validateRequest(request);

        String loadId = request.getId();
        String customerId = request.getCustomerId();

        // Ignore duplicate load IDs for the same customer
        if (transactionRepository.existsByLoadIdAndCustomerId(loadId, customerId)) {
            log.debug("Duplicate load {} for customer {} — ignoring", loadId, customerId);
            return null;
        }

        BigDecimal amount = new BigDecimal(request.getLoadAmount().replace("$", ""));
        Instant time = Instant.parse(request.getTime());

        //Set up boundaries for the transaction we are attempting to process
        LocalDate date = time.atZone(ZoneOffset.UTC).toLocalDate();
        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant weekStart = monday.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant weekEnd = monday.plusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Check velocity limits against previously accepted loads
        long dailyCount = transactionRepository.countAcceptedLoads(customerId, dayStart, dayEnd);
        BigDecimal dailyTotal = transactionRepository.sumAcceptedAmount(customerId, dayStart, dayEnd);
        BigDecimal weeklyTotal = transactionRepository.sumAcceptedAmount(customerId, weekStart, weekEnd);

        boolean accepted = dailyCount < DAILY_LOAD_LIMIT
                && dailyTotal.add(amount).compareTo(DAILY_AMOUNT_LIMIT) <= 0
                && weeklyTotal.add(amount).compareTo(WEEKLY_AMOUNT_LIMIT) <= 0;

        if (!accepted) {
            StringBuilder reason = new StringBuilder("Declined: ");
            if (dailyCount >= DAILY_LOAD_LIMIT) reason.append("daily load count exceeded; ");
            if (dailyTotal.add(amount).compareTo(DAILY_AMOUNT_LIMIT) > 0) reason.append("daily amount limit exceeded; ");
            if (weeklyTotal.add(amount).compareTo(WEEKLY_AMOUNT_LIMIT) > 0) reason.append("weekly amount limit exceeded; ");
            log.info("Load {} for customer {}: {}", loadId, customerId, reason);
        }

        // Persist the attempt in order to track for duplicates
        Transaction transaction = new Transaction();
        transaction.setLoadId(loadId);
        transaction.setCustomerId(customerId);
        transaction.setAmount(amount);
        transaction.setTime(time);
        transaction.setAccepted(accepted);
        transactionRepository.save(transaction);

        // Build response
        LoadResponse response = new LoadResponse();
        response.setId(loadId);
        response.setCustomerId(customerId);
        response.setAccepted(accepted);

        log.debug("Load {} for customer {}: accepted={} (dailyCount={}, dailyTotal={}, weeklyTotal={})",
                loadId, customerId, accepted, dailyCount, dailyTotal, weeklyTotal);
        return response;
    }

    private void validateRequest(LoadRequest request) {
        if (request.getId() == null || request.getId().isBlank()) {
            throw new InvalidLoadRequestException("Load request id is required");
        }
        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            throw new InvalidLoadRequestException("Customer id is required");
        }
        if (request.getLoadAmount() == null || !request.getLoadAmount().matches("^\\$\\d+(\\.\\d{1,2})?$")) {
            throw new InvalidLoadRequestException("Load amount must be in format $X.XX, got: " + request.getLoadAmount());
        }
        if (request.getTime() == null || request.getTime().isBlank()) {
            throw new InvalidLoadRequestException("Time is required");
        }
        try {
            Instant.parse(request.getTime());
        } catch (Exception e) {
            throw new InvalidLoadRequestException("Invalid time format: " + request.getTime());
        }
    }
}
