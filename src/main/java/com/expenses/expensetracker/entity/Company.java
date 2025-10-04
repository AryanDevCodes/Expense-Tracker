package com.expenses.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(exclude = {"employees", "approvalRules"})
@Entity
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    @Column(name = "default_currency", nullable = false)
    private String defaultCurrency;

    @Column(length = 500)
    private String address;

    @Column(name = "contact_email")
    private String contactEmail;

    @OneToMany(mappedBy = "company")
    private Set<User> employees = new HashSet<>();

    @OneToMany(mappedBy = "company")
    private Set<ApprovalRule> approvalRules = new HashSet<>();

    // Core workflow settings
    private boolean requireManagerApproval = true;
    private boolean allowMultiCurrency = true;

    // Approval thresholds
    private BigDecimal autoApprovalThreshold;
    private BigDecimal cfoApprovalThreshold;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Business logic methods
    public boolean isValidCurrency(String currency) {
        return defaultCurrency.equals(currency) || allowMultiCurrency;
    }

    public boolean requiresManagerApproval() {
        return requireManagerApproval;
    }

    // Additional getter method needed by controllers
    public String getCurrency() {
        return this.defaultCurrency;
    }
}
