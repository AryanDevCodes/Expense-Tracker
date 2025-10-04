package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.entity.User;
import com.expenses.expensetracker.entity.Company;
import com.expenses.expensetracker.service.AuthenticationService;
import com.expenses.expensetracker.service.CompanyService;
import com.expenses.expensetracker.service.UserService;
import com.expenses.expensetracker.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User signupFirstAdmin(String username, String email, String password, String country) {
        // Check if this is really first signup
        if (companyService.count() > 0) {
            throw new IllegalStateException("Company already exists. Use regular signup.");
        }

        // Create company with country's currency
        Company company = companyService.createCompanyWithCurrency("Default Company", country);

        // Create admin user
        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setCompany(company);

        // Assign ADMIN role
        admin = userService.createUser(admin);
        userService.assignRole(admin.getId(), "ADMIN");

        return admin;
    }

    @Override
    public User signupUser(String username, String email, String password, Long companyId) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setCompany(companyService.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Company not found")));

        // By default, assign EMPLOYEE role
        user = userService.createUser(user);
        userService.assignRole(user.getId(), "EMPLOYEE");

        return user;
    }

    @Override
    public User authenticate(String username, String password) {
        User user = userService.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        throw new IllegalArgumentException("Invalid credentials");
    }
}
