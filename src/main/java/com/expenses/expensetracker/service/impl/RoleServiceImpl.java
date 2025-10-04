package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.entity.Role;
import com.expenses.expensetracker.repository.RoleRepository;
import com.expenses.expensetracker.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public Role createRole(String name, boolean canCreateEmployees, boolean canManageRoles,
                         boolean canSubmitExpense, boolean canApproveExpenses) {
        Role role = new Role();
        role.setName(name);
        role.setCanCreateEmployees(canCreateEmployees);
        role.setCanManageRoles(canManageRoles);
        role.setCanSubmitExpense(canSubmitExpense);
        role.setCanApproveExpenses(canApproveExpenses);
        return roleRepository.save(role);
    }

    @Override
    public Role getOrCreateDefaultRole(String roleName) {
        return roleRepository.findByName(roleName)
            .orElseGet(() -> {
                switch (roleName.toUpperCase()) {
                    case "ADMIN":
                        return createRole("ADMIN", true, true, true, true);
                    case "MANAGER":
                        return createRole("MANAGER", false, false, true, true);
                    case "EMPLOYEE":
                        return createRole("EMPLOYEE", false, false, true, false);
                    default:
                        throw new IllegalArgumentException("Unknown role: " + roleName);
                }
            });
    }

    @Override
    public void updateRolePermissions(Long roleId, List<String> permissions) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        // Update permissions based on the list
        role.setCanCreateEmployees(permissions.contains("CREATE_EMPLOYEES"));
        role.setCanManageRoles(permissions.contains("MANAGE_ROLES"));
        role.setCanSubmitExpense(permissions.contains("SUBMIT_EXPENSE"));
        role.setCanApproveExpenses(permissions.contains("APPROVE_EXPENSES"));
        role.setCanViewAllExpenses(permissions.contains("VIEW_ALL_EXPENSES"));
        role.setCanConfigureRules(permissions.contains("CONFIGURE_RULES"));
        role.setCanOverrideApprovals(permissions.contains("OVERRIDE_APPROVALS"));

        roleRepository.save(role);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    public Optional<Role> findById(Long roleId) {
        return roleRepository.findById(roleId);
    }

    @Override
    public Role createRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public Role updateRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public void deleteRole(Long roleId) {
        roleRepository.deleteById(roleId);
    }

    @Override
    public boolean existsByName(String name) {
        return roleRepository.findByName(name).isPresent();
    }
}
