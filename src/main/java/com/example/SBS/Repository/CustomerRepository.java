package com.example.SBS.Repository;

import com.example.SBS.Entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhoneNumber(String phoneNumber);  // To ensure uniqueness
    Optional<Customer> findByBankAccountNumber(String bankAccountNumber);  // Ensure account number is unique

}
