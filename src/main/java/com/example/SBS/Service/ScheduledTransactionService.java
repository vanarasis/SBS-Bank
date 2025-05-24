package com.example.SBS.Service;

import com.example.SBS.Entity.Customer;
import com.example.SBS.Entity.ScheduledTransaction;
import com.example.SBS.Exception.CustomerNotFoundException;
import com.example.SBS.Repository.CustomerRepository;
import com.example.SBS.Repository.ScheduledTransactionRepository;
import com.example.SBS.Service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledTransactionService {

    @Autowired
    private  ScheduledTransactionRepository scheduledTransactionRepository;
    @Autowired
    private  CustomerService customerService;
    @Autowired
    private CustomerRepository customerRepository;


    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void processScheduledTransactions() {
        List<ScheduledTransaction> pending = scheduledTransactionRepository
                .findByScheduledDateBeforeAndStatus(
                        java.time.LocalDateTime.now(),
                        ScheduledTransaction.Status.PENDING
                );

        for (ScheduledTransaction txn : pending) {
            try {
                customerService.transferMoney(
                        txn.getFromAccount(),
                        txn.getToAccount(),
                        txn.getAmount()
                );
                txn.setStatus(ScheduledTransaction.Status.COMPLETED);
            } catch (Exception e) {
                txn.setStatus(ScheduledTransaction.Status.FAILED);
            }
            scheduledTransactionRepository.save(txn);
        }
    }

    public ScheduledTransaction scheduleTransaction(ScheduledTransaction txn) {
        // 1. Fetch the customer
        Customer customer = customerRepository.findByBankAccountNumber(txn.getFromAccount())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        // 2. Get total amount already scheduled for the same day
        LocalDate date = txn.getScheduledDate().toLocalDate();
        BigDecimal totalScheduledForDay = scheduledTransactionRepository
                .findByFromAccountAndScheduledDateBetweenAndStatus(
                        txn.getFromAccount(),
                        date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay(),
                        ScheduledTransaction.Status.PENDING
                )
                .stream()
                .map(ScheduledTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Get total already transferred today (from actual transactions)
        BigDecimal transferredToday = customerService.getTotalTransferredToday(txn.getFromAccount());

        // 4. Calculate remaining daily limit
        BigDecimal totalUsed = totalScheduledForDay.add(transferredToday);
        BigDecimal remainingLimit = customer.getDailyLimit().subtract(totalUsed);

        if (txn.getAmount().compareTo(remainingLimit) > 0) {
            throw new RuntimeException("Scheduled transaction denied: exceeds remaining daily limit. Remaining: ₹" + remainingLimit);
        }

        // 5. Set status and save
        txn.setStatus(ScheduledTransaction.Status.PENDING);
        return scheduledTransactionRepository.save(txn);
    }

}
