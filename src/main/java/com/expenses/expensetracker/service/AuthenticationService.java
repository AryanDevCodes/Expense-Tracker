package com.expenses.expensetracker.service;

import com.expenses.expensetracker.entity.User;

public interface AuthenticationService {
    // Required for first login/signup with company creation
    User signupFirstAdmin(String username, String email, String password, String country);

    // Regular user signup (needs existing company)
    User signupUser(String username, String email, String password, Long companyId);

    // Required for login
    User authenticate(String username, String password);
}
