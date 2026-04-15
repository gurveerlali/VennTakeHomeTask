package org.venn.takeHomeTest.repository;

import org.venn.takeHomeTest.repository.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByLoadIdAndCustomerId(String loadId, String customerId);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM Transaction l " +
            "WHERE l.customerId = :customerId AND l.accepted = true " +
            "AND l.time >= :start AND l.time < :end")
    BigDecimal sumAcceptedAmount(@Param("customerId") String customerId,
                                 @Param("start") Instant start,
                                 @Param("end") Instant end);

    @Query("SELECT COUNT(l) FROM Transaction l " +
            "WHERE l.customerId = :customerId AND l.accepted = true " +
            "AND l.time >= :start AND l.time < :end")
    long countAcceptedLoads(@Param("customerId") String customerId,
                            @Param("start") Instant start,
                            @Param("end") Instant end);
}
