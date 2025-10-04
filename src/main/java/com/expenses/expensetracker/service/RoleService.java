package com.expenses.expensetracker.service;

import com.expenses.expensetracker.entity.Role;
import java.util.List;
import java.util.Optional;

public interface RoleService {
    // Core role management
    Role createRole(String name, boolean canCreateEmployees, boolean canManageRoles,
                   boolean canSubmitExpense, boolean canApproveExpenses);

    // Required for admin functionality
    Role getOrCreateDefaultRole(String roleName);
    void updateRolePermissions(Long roleId, List<String> permissions);

    // Required for user management
    Optional<Role> findByName(String name);
    List<Role> getAllRoles();

    List<Role> findAll();
    Optional<Role> findById(Long roleId);
    Role createRole(Role role);
    Role updateRole(Role role);
    void deleteRole(Long roleId);
    boolean existsByName(String name);
}
