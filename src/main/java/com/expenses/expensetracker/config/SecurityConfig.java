package com.expenses.expensetracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public URLs
                .requestMatchers("/signup", "/login", "/api/public/**", "/css/**", "/js/**", "/static/**").permitAll()

                // Admin-only URLs
                .requestMatchers("/admin/**", "/users/**", "/roles/**", "/companies/**", "/approval-rules/**", "/approver-configs/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Manager and Admin URLs
                .requestMatchers("/expenses/pending", "/expenses/team/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/api/expenses/approve/**", "/api/expenses/reject/**").hasAnyRole("MANAGER", "ADMIN")

                // All authenticated users can access expense functions
                .requestMatchers("/expenses/**", "/api/expenses/**", "/dashboard", "/approval-steps/**").authenticated()

                // All authenticated users
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login")
                .permitAll()
            )
            // Enable CSRF protection
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/ocr/**") // Needed for OCR API calls
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
