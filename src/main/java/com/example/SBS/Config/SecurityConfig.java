package com.example.SBS.Config;

import com.example.SBS.Service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.example.SBS.Security.CustomAuthenticationEntryPoint;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CustomAuthenticationEntryPoint entryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API requests
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/admins/create","/api/admins/login","/api/customers/login").permitAll() // Allow unauthenticated access to create admin
                        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "CUSTOMER") // Allow both ADMIN and CUSTOMER roles
                        .requestMatchers("/api/admins/**").permitAll()
                        .requestMatchers("/", "/index.html","/AdminDashboard.html","/customerDashboard.html","/welcome.html","/services.html","/contact.html").permitAll()
                        //added for testing purpose
                        .requestMatchers("/images/**", "/css/**", "/js/**", "/static/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customers/create").hasRole("ADMIN")
                        .anyRequest().authenticated() // All other requests require authentication
                )
                .formLogin(form -> form.disable()) // Disable form login for APIs
                .httpBasic(Customizer.withDefaults()) // Use basic authentication
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint) // ✅ This is correct
                )
                .authenticationProvider(authenticationProvider()) // Custom Authentication Provider
                .build();
    }


    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


}