package com.example.SBS.Controller;

import com.example.SBS.DTO.MiniStatementRequest;
import com.example.SBS.Entity.Customer;
import com.example.SBS.Entity.Transaction;
import com.example.SBS.Exception.CustomerNotFoundException;
import com.example.SBS.Exception.UnauthorizedAccessException;
import com.example.SBS.Repository.CustomerRepository;
import com.example.SBS.Repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // Get all transactions (Admin only)
    @GetMapping("/all")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new UnauthorizedAccessException("Only admins can view all transactions.");
        }

        return ResponseEntity.ok(transactionRepository.findAll());
    }

    // Get own transactions (Customer)
    @GetMapping("/my")
    public ResponseEntity<List<Transaction>> getMyTransactions() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();

        Customer customer = customerRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        List<Transaction> transactions = transactionRepository
                .findByFromAccountOrToAccount(customer.getBankAccountNumber(), customer.getBankAccountNumber());

        return ResponseEntity.ok(transactions);
    }

    // Get transactions of a particular customer (Admin only)
    @GetMapping("/customer/{accountNumber}")
    public ResponseEntity<List<Transaction>> getTransactionsByCustomer(@PathVariable String accountNumber) {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new UnauthorizedAccessException("Only admins can view transactions of a specific customer.");
        }

        Customer customer = customerRepository.findByBankAccountNumber(accountNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        List<Transaction> transactions = transactionRepository
                .findByFromAccountOrToAccount(customer.getBankAccountNumber(), customer.getBankAccountNumber());

        return ResponseEntity.ok(transactions);
    }


    // Get mini statement between fromDate and toDate (Admin can access for any user, Customer can access only their own)
    @PostMapping("/mini-statement")
    public ResponseEntity<List<Transaction>> getMiniStatement(@RequestBody MiniStatementRequest request) {
        String accountNumber = request.getAccountNumber();
        LocalDate fromDate = request.getFromDate();  // <-- Make sure these are declared
        LocalDate toDate = request.getToDate();

        // Log the adjusted date range
        System.out.println("From Date: " + fromDate);
        System.out.println("To Date: " + toDate);

        // Get current user's roles
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!isAdmin) {
            Customer customer = customerRepository.findByPhoneNumber(currentUsername)
                    .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

            if (!customer.getBankAccountNumber().equals(accountNumber)) {
                throw new UnauthorizedAccessException("You can only access your own transactions.");
            }
        }

        // ✅ Now use fromDate and toDate here — they are in scope
        List<Transaction> transactions = transactionRepository
                .findTransactionsByAccountAndDateRange(accountNumber, fromDate, toDate);

        System.out.println("Fetched Transactions: " + transactions);

        return ResponseEntity.ok(transactions);
    }

}
