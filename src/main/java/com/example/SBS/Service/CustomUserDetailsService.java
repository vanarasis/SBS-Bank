package com.example.SBS.Service;

import com.example.SBS.Entity.Admin;
import com.example.SBS.Entity.Customer;
import com.example.SBS.Repository.AdminRepository;
import com.example.SBS.Repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check admin first
        Admin admin = adminRepository.findByUsername(username).orElse(null);
        if (admin != null) {
            return User.builder()
                    .username(admin.getUsername())
                    .password(admin.getPasswordHash())  // Hashed
                    .roles(admin.getRole().name())      // "ADMIN"
                    .build();
        }

        // Then check customer
        Customer customer = customerRepository.findByPhoneNumber(username).orElse(null);
        if (customer != null) {
            return User.builder()
                    .username(customer.getPhoneNumber())
                    .password(customer.getPasswordHash())   // Hashed
                    .roles("CUSTOMER")
                    .build();
        }

        throw new UsernameNotFoundException("User not found with username or phone: " + username);
    }
}
