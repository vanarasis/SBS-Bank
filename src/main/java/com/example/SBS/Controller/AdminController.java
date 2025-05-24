package com.example.SBS.Controller;

import com.example.SBS.DTO.AdminDTO;
import com.example.SBS.Entity.Admin;
import com.example.SBS.Entity.Customer;
import com.example.SBS.Entity.Transaction;
import com.example.SBS.Exception.CustomerNotFoundException;
import com.example.SBS.Exception.UnauthorizedAccessException;
import com.example.SBS.Repository.CustomerRepository;
import com.example.SBS.Repository.TransactionRepository;
import com.example.SBS.Service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;


    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/create")
    public ResponseEntity<?> createAdmin(@RequestBody AdminDTO adminDTO) {
        Admin created = adminService.createAdmin(adminDTO);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAdminById(@PathVariable Long id) {
        Admin admin = adminService.getAdminById(id);
        if (admin == null) {
            throw new RuntimeException("Admin with ID " + id + " not found.");
        }
        return ResponseEntity.ok(admin);
    }

    @GetMapping
    public ResponseEntity<?> getAllAdmins() {
        List<Admin> admins = adminService.getAllAdmins();
        return ResponseEntity.ok(admins);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long id) {
        Admin admin = adminService.getAdminById(id);
        if (admin == null) {
            throw new RuntimeException("Admin with ID " + id + " not found.");
        }
        adminService.deleteAdmin(id);
        return ResponseEntity.ok("Admin deleted successfully");
    }


    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<?> getTransactions(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        Customer customer = customerRepository.findByBankAccountNumber(accountNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        if (!isAdmin && !customer.getPhoneNumber().equals(username)) {
            throw new UnauthorizedAccessException("Unauthorized access to transactions.");
        }

        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Transaction> transactions = transactionRepository
                .findByFromAccountOrToAccount(accountNumber, accountNumber, pageable);

        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(@RequestBody AdminDTO loginDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getUsername(), loginDTO.getPassword()
                    )
            );

            // Check if user has admin role
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                return ResponseEntity.status(403).body("Access denied: not an admin");
            }

            return ResponseEntity.ok("Login successful");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }


    @GetMapping("/viewcustomers")
    public ResponseEntity<?> getAllCustomers() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new UnauthorizedAccessException("Access denied: Only admins can view all customers.");
        }

        List<Customer> customers = customerRepository.findAll();
        return ResponseEntity.ok(customers);
    }


}
