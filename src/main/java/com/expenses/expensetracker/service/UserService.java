package com.expenses.expensetracker.service;

import com.expenses.expensetracker.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    // Core user management
    User createUser(User user);
    void assignRole(Long userId, String roleName);
    void setManager(Long employeeId, Long managerId);

    // Required for admin
    List<User> getEmployeesByManager(Long managerId);
    List<User> getUsersByRole(String roleName);
    List<User> findAll(); // Add missing method

    // Required for authentication
    User findByUsername(String username);
    Optional<User> findById(Long userId);
    boolean existsByUsername(String username);

    // CRUD operations
    User updateUser(User user);
    void deleteUser(Long userId);
}