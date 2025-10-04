package com.expenses.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "approver_configs")
@Data
public class ApproverConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_rule_id", nullable = false)
    private ApprovalRule approvalRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "min_amount")
    private BigDecimal minAmount;

    @Column(name = "max_amount")
    private BigDecimal maxAmount;

    // Role-specific flags
    @Column(name = "is_manager_step")
    private boolean isManagerStep = false;

    @Column(name = "is_finance_step")
    private boolean isFinanceStep = false;

    @Column(name = "is_director_step")
    private boolean isDirectorStep = false;

    @Column(name = "is_cfo_step")
    private boolean isCfoStep = false;

    // Helper methods
    public boolean isApplicableForAmount(BigDecimal amount) {
        if (minAmount == null && maxAmount == null) return true;
        boolean aboveMin = minAmount == null || amount.compareTo(minAmount) >= 0;
        boolean belowMax = maxAmount == null || amount.compareTo(maxAmount) <= 0;
        return aboveMin && belowMax;
    }

    public boolean isRequiredForExpense(Expense expense) {
        if (!isApplicableForAmount(expense.getAmount())) {
            return false;
        }

        if (isManagerStep) {
            return expense.getSubmitter().getManager() != null;
        }

        return true;
    }

    // Ensure proper sequence based on role
    @PrePersist
    @PreUpdate
    protected void validateSequence() {
        if (isManagerStep && sequence != 1) {
            sequence = 1; // Manager must be first if required
        }
        if (isFinanceStep && sequence < 2) {
            sequence = 2; // Finance comes after manager
        }
        if (isDirectorStep && sequence < 3) {
            sequence = 3; // Director comes after finance
        }
    }

    // Additional setter methods needed by controllers
    public void setMinAmountThreshold(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public void setMaxAmountThreshold(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public void setIsManagerApprover(boolean isManagerStep) {
        this.isManagerStep = isManagerStep;
    }

    public void setIsAutoApprover(boolean isAutoApprover) {
        // This would be a flag to indicate auto-approval capability
        this.isCfoStep = isAutoApprover;
    }

    public Boolean getIsManagerApprover() {
        return this.isManagerStep;
    }

    public Boolean getIsAutoApprover() {
        return this.isCfoStep;
    }
}
