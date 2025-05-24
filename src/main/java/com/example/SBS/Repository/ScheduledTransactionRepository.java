package com.example.SBS.Repository;

import com.example.SBS.Entity.ScheduledTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledTransactionRepository extends JpaRepository<ScheduledTransaction, Long> {
    List<ScheduledTransaction> findByScheduledDateBeforeAndStatus(LocalDateTime now, ScheduledTransaction.Status status);
    List<ScheduledTransaction> findByFromAccountAndScheduledDateBetweenAndStatus(
            String fromAccount,
            LocalDateTime start,
            LocalDateTime end,
            ScheduledTransaction.Status status
    );

}
