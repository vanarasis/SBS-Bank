package com.example.SBS.Repository;

import com.example.SBS.Entity.Transaction;
//import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.fromAccount = :fromAccount AND t.date = :date")
    BigDecimal getTotalTransferredToday(String fromAccount, LocalDate date);

    List<Transaction> findByFromAccountOrToAccount(String fromAcc, String toAcc);

    List<Transaction> findByFromAccountAndDate(String fromAccount, LocalDate date);


    List<Transaction> findByFromAccountOrToAccountAndDateBetween(String fromAcc, String toAcc, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE (t.fromAccount = :accountNumber OR t.toAccount = :accountNumber) AND t.date BETWEEN :fromDate AND :toDate")
    List<Transaction> findTransactionsByAccountAndDateRange(@Param("accountNumber") String accountNumber,
                                                            @Param("fromDate") LocalDate fromDate,
                                                            @Param("toDate") LocalDate toDate);

    Page<Transaction> findByFromAccountOrToAccount(String fromAccount, String toAccount, Pageable pageable);
}
