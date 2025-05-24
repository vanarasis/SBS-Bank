package com.example.SBS.Controller;

import com.example.SBS.DTO.*;
import com.example.SBS.Entity.Customer;
import com.example.SBS.Entity.Transaction;
import com.example.SBS.Exception.CustomerNotFoundException;
import com.example.SBS.Exception.UnauthorizedAccessException;
import com.example.SBS.Repository.CustomerRepository;
import com.example.SBS.Repository.TransactionRepository;
import com.example.SBS.Service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Add this bean

    @PostMapping("/create")
    public ResponseEntity<?> createCustomer(@RequestBody CustomerDTO customerDTO) {
        // Get logged-in user's roles
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        // Restrict access to admin only
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CustomerService.ErrorResponse("Access denied. Only admins can create customers."));
        }

        // Delegate creation to the service
        return customerService.createCustomer(customerDTO);
    }



    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<Customer> optionalCustomer = customerRepository.findByPhoneNumber(loginRequest.getPhoneNumber());

        if (optionalCustomer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid phone number or password");
        }

        Customer customer = optionalCustomer.get();

        if (!passwordEncoder.matches(loginRequest.getPassword(), customer.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid phone number or password");
        }

        // In real scenarios, you should return a JWT token here instead of just a message
        return ResponseEntity.ok("Login successful for customer: " + customer.getName());
    }

    @GetMapping("/balance/{accountNumber}")
    public ResponseEntity<?> checkBalance(@PathVariable String accountNumber) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            Customer customer = customerRepository.findByBankAccountNumber(accountNumber)
                    .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
            return ResponseEntity.ok(customer.getBalance());
        }

        Customer customer = customerRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        if (!customer.getBankAccountNumber().equals(accountNumber)) {
            throw new UnauthorizedAccessException("Unauthorized access to this account.");
        }

        return ResponseEntity.ok(customer.getBalance());
    }


    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(@RequestBody TransferRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (request.getFromAccount().equals(request.getToAccount())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("From and To account numbers cannot be the same");
        }

        Customer customer = null;

        if (!isAdmin) {
            customer = customerRepository.findByPhoneNumber(username)
                    .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

            if (!customer.getBankAccountNumber().equals(request.getFromAccount())) {
                throw new UnauthorizedAccessException("Unauthorized transfer attempt.");
            }

            BigDecimal totalTransferredToday = customerService.getTotalTransferredToday(request.getFromAccount());
            BigDecimal dailyLimit = customer.getDailyLimit();
            BigDecimal remainingLimit = dailyLimit.subtract(totalTransferredToday);

            if (request.getAmount().compareTo(remainingLimit) > 0) {
                return ResponseEntity.ok("Transfer denied: Exceeded daily transfer limit. Remaining limit: ₹" + remainingLimit);
            }
        }

        Optional<Customer> fromCustomer = customerRepository.findByBankAccountNumber(request.getFromAccount());
        if (fromCustomer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("From account not found");
        }

        Optional<Customer> toCustomer = customerRepository.findByBankAccountNumber(request.getToAccount());
        if (toCustomer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("To account not found");
        }

        // Use the txnId returned from the service
        String txnId = customerService.transferMoney(
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmount()
        );

        InvoiceResponseDTO invoice = new InvoiceResponseDTO(
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmount(),
                LocalDateTime.now(),
                txnId,
                "Success"
        );

        return ResponseEntity.ok(invoice);
    }






    @GetMapping("/{accountNumber}/details")
    public ResponseEntity<Customer> getCustomerDetails(@PathVariable String accountNumber) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(
                    customerService.getCustomerDetails(accountNumber)
            );
        }

        Customer customer = customerRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        if (!customer.getBankAccountNumber().equals(accountNumber)) {
            throw new UnauthorizedAccessException("Unauthorized access to this account.");
        }

        return ResponseEntity.ok(customer);
    }

    @PutMapping("/{accountNumber}/update")
    public ResponseEntity<?> updateCustomerDetails(@PathVariable String accountNumber,
                                                   @RequestBody UpdateCustomerDTO updateDTO) {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new UnauthorizedAccessException("Only admin can update customer details.");
        }

        Customer customer = customerRepository.findByBankAccountNumber(accountNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        if (updateDTO.getName() != null) {
            customer.setName(updateDTO.getName());
        }

        if (updateDTO.getAddress() != null) {
            customer.setAddress(updateDTO.getAddress());
        }

        customerRepository.save(customer);
        return ResponseEntity.ok("Customer details updated successfully");
    }

    @PutMapping("/{accountNumber}/daily-limit")
    public ResponseEntity<String> updateDailyLimit(@PathVariable String accountNumber,
                                                   @RequestBody Map<String, BigDecimal> body) {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new UnauthorizedAccessException("Only admins can update daily limit.");
        }

        BigDecimal newLimit = body.get("newLimit");

        Customer customer = customerRepository.findByBankAccountNumber(accountNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        customer.setDailyLimit(newLimit);
        customerRepository.save(customer);

        return ResponseEntity.ok("Daily limit updated to " + newLimit);
    }


    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody DepositWithdrawRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        Customer customer = customerRepository.findByBankAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        if (!isAdmin && !customer.getPhoneNumber().equals(username)) {
            throw new UnauthorizedAccessException("Unauthorized deposit attempt.");
        }

        customer.setBalance(customer.getBalance().add(request.getAmount()));
        customerRepository.save(customer);

        // Log transaction
        Transaction txn = new Transaction();
        txn.setTransactionId("TXN" + System.currentTimeMillis());
        txn.setFromAccount(null); // Not applicable
        txn.setToAccount(request.getAccountNumber());
        txn.setAmount(request.getAmount());
        txn.setDate(LocalDate.now());
        txn.setType("DEPOSIT");
        txn.setStatus("SUCCESS");

        transactionRepository.save(txn);

        return ResponseEntity.ok("Deposited ₹" + request.getAmount() + " successfully");
    }


    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody DepositWithdrawRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        Customer customer = customerRepository.findByBankAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        if (!isAdmin && !customer.getPhoneNumber().equals(username)) {
            throw new UnauthorizedAccessException("Unauthorized withdraw attempt.");
        }

        if (customer.getBalance().compareTo(request.getAmount()) < 0) {
            // Log failed transaction
            Transaction txn = new Transaction();
            txn.setTransactionId("TXN" + System.currentTimeMillis());
            txn.setFromAccount(request.getAccountNumber());
            txn.setToAccount(null); // Not applicable
            txn.setAmount(request.getAmount());
            txn.setDate(LocalDate.now());
            txn.setType("WITHDRAW");
            txn.setStatus("FAILED");

            transactionRepository.save(txn);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Insufficient balance. Available: ₹" + customer.getBalance());
        }

        customer.setBalance(customer.getBalance().subtract(request.getAmount()));
        customerRepository.save(customer);

        // Log successful transaction
        Transaction txn = new Transaction();
        txn.setTransactionId("TXN" + System.currentTimeMillis());
        txn.setFromAccount(request.getAccountNumber());
        txn.setToAccount(null); // Not applicable
        txn.setAmount(request.getAmount());
        txn.setDate(LocalDate.now());
        txn.setType("WITHDRAW");
        txn.setStatus("SUCCESS");

        transactionRepository.save(txn);

        return ResponseEntity.ok("Withdrew ₹" + request.getAmount() + " successfully");
    }

    @PostMapping("/account-number")
    public ResponseEntity<?> getAccountNumber(@RequestBody LoginRequest loginRequest) {
        Optional<Customer> optionalCustomer = customerRepository.findByPhoneNumber(loginRequest.getPhoneNumber());

        if (optionalCustomer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid phone number or password");
        }

        Customer customer = optionalCustomer.get();

        if (!passwordEncoder.matches(loginRequest.getPassword(), customer.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid phone number or password");
        }

        return ResponseEntity.ok(Map.of("accountNumber", customer.getBankAccountNumber()));
    }


    @PostMapping("/verify-recipient")
    public ResponseEntity<?> verifyRecipient(@RequestBody Map<String, String> body) {
        String phoneNumber = body.get("phoneNumber");
        String loggedInUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // Ensure user has CUSTOMER role
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isCustomer = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER"));
        if (!isCustomer) {
            throw new UnauthorizedAccessException("Only customers can access this feature.");
        }

        // Prevent user from checking themselves
        if (loggedInUsername.equals(phoneNumber)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("You cannot verify your own phone number.");
        }

        Optional<Customer> optionalRecipient = customerRepository.findByPhoneNumber(phoneNumber);
        if (optionalRecipient.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Recipient not found.");
        }

        Customer recipient = optionalRecipient.get();

        return ResponseEntity.ok(Map.of(
                "name", recipient.getName(),
                "accountNumber", recipient.getBankAccountNumber()
        ));
    }


    @PostMapping("/transfer-by-phone")
    public ResponseEntity<?> transferByPhone(@RequestBody PhoneTransferRequest request) {
        String senderPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            throw new UnauthorizedAccessException("Only customers can perform this transaction.");
        }

        if (senderPhone.equals(request.getRecipientPhoneNumber())) {
            return ResponseEntity.badRequest().body("Cannot transfer to your own number.");
        }

        Customer sender = customerRepository.findByPhoneNumber(senderPhone)
                .orElseThrow(() -> new CustomerNotFoundException("Sender not found"));

        Customer recipient = customerRepository.findByPhoneNumber(request.getRecipientPhoneNumber())
                .orElseThrow(() -> new CustomerNotFoundException("Recipient not found"));

        BigDecimal totalTransferredToday = customerService.getTotalTransferredToday(sender.getBankAccountNumber());
        BigDecimal remainingLimit = sender.getDailyLimit().subtract(totalTransferredToday);

        if (request.getAmount().compareTo(remainingLimit) > 0) {
            return ResponseEntity.ok("Transfer denied: Exceeded daily transfer limit. Remaining limit: ₹" + remainingLimit);
        }

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            return ResponseEntity.badRequest().body("Insufficient balance. Available: ₹" + sender.getBalance());
        }

        // Perform the transfer
        String txnId = customerService.transferMoney(
                sender.getBankAccountNumber(),
                recipient.getBankAccountNumber(),
                request.getAmount()
        );

        InvoiceResponseDTO invoice = new InvoiceResponseDTO(
                sender.getBankAccountNumber(),
                recipient.getBankAccountNumber(),
                request.getAmount(),
                LocalDateTime.now(),
                txnId,
                "Success"
        );

        return ResponseEntity.ok(invoice);
    }


}
