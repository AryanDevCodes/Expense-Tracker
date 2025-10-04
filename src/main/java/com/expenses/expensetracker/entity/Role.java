package com.expenses.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@EqualsAndHashCode(exclude = {"users"})
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    // Specific permissions as per requirements
    private boolean canCreateEmployees = false;
    private boolean canManageRoles = false;
    private boolean canSubmitExpense = false;
    private boolean canViewOwnExpenses = false;
    private boolean canApproveExpenses = false;
    private boolean canViewTeamExpenses = false;
    private boolean canEscalate = false;
    private boolean canOverrideApprovals = false;
    private boolean canConfigureRules = false;
    private boolean canViewAllExpenses = false;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    // Factory methods for required roles
    public static Role createAdminRole() {
        Role role = new Role();
        role.setName("ADMIN");
        role.setCanCreateEmployees(true);
        role.setCanManageRoles(true);
        role.setCanConfigureRules(true);
        role.setCanViewAllExpenses(true);
        role.setCanOverrideApprovals(true);
        return role;
    }

    public static Role createManagerRole() {
        Role role = new Role();
        role.setName("MANAGER");
        role.setCanApproveExpenses(true);
        role.setCanViewTeamExpenses(true);
        role.setCanEscalate(true);
        role.setCanSubmitExpense(true);
        role.setCanViewOwnExpenses(true);
        return role;
    }

    public static Role createEmployeeRole() {
        Role role = new Role();
        role.setName("EMPLOYEE");
        role.setCanSubmitExpense(true);
        role.setCanViewOwnExpenses(true);
        return role;
    }
}
