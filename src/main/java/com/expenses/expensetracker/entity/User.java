package com.expenses.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users",
    indexes = {
        @Index(name = "idx_user_manager", columnList = "manager_id"),
        @Index(name = "idx_user_company", columnList = "company_id"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_username", columnList = "username")
    }
)
@Getter
@Setter
@EqualsAndHashCode(exclude = {"roles", "subordinates"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;


    @OneToMany(mappedBy = "manager")
    private Set<User> subordinates = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Helper methods for role management
    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    public boolean isManager() {
        return hasRole("MANAGER");
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean canApproveExpense() {
        return roles.stream().anyMatch(role -> role.isCanApproveExpenses());
    }

    public boolean canSubmitExpense() {
        return roles.stream().anyMatch(role -> role.isCanSubmitExpense());
    }

    // Manager relationship methods
    public boolean isManagerOf(User employee) {
        return subordinates.contains(employee);
    }

    public boolean hasManager() {
        return manager != null;
    }

    // Company currency helper
    public String getCompanyCurrency() {
        return company != null ? company.getDefaultCurrency() : null;
    }
}
