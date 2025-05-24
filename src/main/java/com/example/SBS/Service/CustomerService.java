package com.example.SBS.Service;

import com.example.SBS.DTO.CustomerDTO;
import com.example.SBS.Entity.Customer;
import com.example.SBS.Entity.AccountType;
import com.example.SBS.Entity.Transaction;
import com.example.SBS.Repository.CustomerRepository;
import com.example.SBS.Repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionRepository transactionRepository;

    public ResponseEntity<?> createCustomer(CustomerDTO customerDTO) {
        try {
            if (customerRepository.findByPhoneNumber(customerDTO.getPhoneNumber()).isPresent()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Phone number is already in use."));
            }

            if (customerRepository.findByBankAccountNumber(customerDTO.getBankAccountNumber()).isPresent()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Bank account number is already in use."));
            }

            // Handle invalid account type gracefully
            String accountTypeInput = customerDTO.getAccountType().toUpperCase();
            AccountType accountType;
            try {
                accountType = AccountType.valueOf(accountTypeInput);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Invalid account type. Valid values are: SAVINGS, CHECKING, CURRENT."));
            }

            BigDecimal initialBalance = customerDTO.getBalance() != null ? customerDTO.getBalance() : BigDecimal.ZERO;

            Customer customer = new Customer(
                    customerDTO.getName(),
                    customerDTO.getEmail(),
                    customerDTO.getPhoneNumber(),
                    customerDTO.getAddress(),
                    customerDTO.getBankAccountNumber(),
                    "SBS BANK",
                    customerDTO.getIfscCode(),
                    accountType,
                    passwordEncoder.encode(customerDTO.getPasswordHash())
            );

            customer.setBalance(initialBalance);
            Customer savedCustomer = customerRepository.save(customer);

            return ResponseEntity.ok(savedCustomer);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to create customer: " + e.getMessage()));
        }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }



   /* public static class ErrorResponse {
        private String message;
        public ErrorResponse(String message) { this.message = message; }
        public String getMessage() { return message; }
    }

    */



    public BigDecimal getBalance(String accountNumber) {
        Customer customer = customerRepository.findByBankAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return customer.getBalance();
    }

    public String transferMoney(String fromAcc, String toAcc, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        Customer sender = customerRepository.findByBankAccountNumber(fromAcc)
                .orElseThrow(() -> new IllegalArgumentException("Sender account not found"));

        Customer receiver = customerRepository.findByBankAccountNumber(toAcc)
                .orElseThrow(() -> new IllegalArgumentException("Receiver account not found"));

        // Check daily limit
        BigDecimal totalTransferredToday = transactionRepository.getTotalTransferredToday(fromAcc, LocalDate.now());
        if (totalTransferredToday.add(amount).compareTo(sender.getDailyLimit()) > 0) {
            throw new IllegalArgumentException("Transfer exceeds daily limit.");
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds.");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        customerRepository.save(sender);
        customerRepository.save(receiver);

        // Generate and save transaction
        String txnId = "TXN" + System.currentTimeMillis();

        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAcc);
        transaction.setToAccount(toAcc);
        transaction.setAmount(amount);
        transaction.setDate(LocalDate.now());
        transaction.setTransactionId(txnId);
        transaction.setType("Transfer"); // Fix typo: "Transver" -> "Transfer"
        transaction.setStatus("SUCCESS");

        transactionRepository.save(transaction);

        return txnId;
    }


    public Customer getCustomerDetails(String accountNumber) {
        return customerRepository.findByBankAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    public BigDecimal getTotalTransferredToday(String fromAccount) {
        LocalDate today = LocalDate.now();
            List<Transaction> todayTransactions = transactionRepository.findByFromAccountAndDate(fromAccount, today);
        return todayTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
