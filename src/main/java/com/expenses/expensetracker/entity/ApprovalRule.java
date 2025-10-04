package com.expenses.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_rules")
@Data
public class ApprovalRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    private String name;

    // Amount threshold for rule application
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    // Rule type flags
    private boolean requiresManagerFirst = true;

    // Percentage rule configuration
    private Integer requiredPercentage = 60; // Default 60% as per requirements

    // Specific approver (CFO) configuration
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cfo_approver_id")
    private User cfoApprover;

    // Hybrid rule configuration
    private boolean isHybridRule = false;
    private boolean isPercentageOrCfo = true; // true = OR, false = AND

    // Approval sequence
    @OneToMany(mappedBy = "approvalRule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence")
    private List<ApproverConfig> approvers = new ArrayList<>();

    // Methods to check rule conditions
    public boolean isApplicableForAmount(BigDecimal amount) {
        if (minAmount == null && maxAmount == null) return true;
        boolean aboveMin = minAmount == null || amount.compareTo(minAmount) >= 0;
        boolean belowMax = maxAmount == null || amount.compareTo(maxAmount) <= 0;
        return aboveMin && belowMax;
    }

    public boolean isApproved(Expense expense) {
        if (!isApplicableForAmount(expense.getAmount())) {
            return false;
        }

        boolean percentageApproved = checkPercentageApproval(expense);
        boolean cfoApproved = checkCfoApproval(expense);

        if (isHybridRule) {
            return isPercentageOrCfo ?
                   (percentageApproved || cfoApproved) :
                   (percentageApproved && cfoApproved);
        }

        return percentageApproved || cfoApproved;
    }

    private boolean checkPercentageApproval(Expense expense) {
        if (requiredPercentage == null) return false;

        long approvedCount = expense.getApprovalSteps().stream()
            .filter(step -> step.getStatus() == ApprovalStep.ApprovalStepStatus.APPROVED)
            .count();

        double percentage = (approvedCount * 100.0) / expense.getApprovalSteps().size();
        return percentage >= requiredPercentage;
    }

    private boolean checkCfoApproval(Expense expense) {
        if (cfoApprover == null) return false;

        return expense.getApprovalSteps().stream()
            .anyMatch(step ->
                step.getApprover().equals(cfoApprover) &&
                step.getStatus() == ApprovalStep.ApprovalStepStatus.APPROVED);
    }

    // Setter methods for admin configuration
    public void setSpecificApprover(User specificApprover) {
        this.cfoApprover = specificApprover;
    }

    public User getSpecificApprover() {
        return this.cfoApprover;
    }

    // Getter method for approvers
    public List<ApproverConfig> getApprovers() {
        return approvers;
    }

    // Additional getter methods needed by controllers
    public Integer getPercentageRule() {
        return this.requiredPercentage;
    }

    public boolean isHybrid() {
        return this.isHybridRule;
    }

    public void setPercentageRule(Integer percentage) {
        this.requiredPercentage = percentage;
    }

    public void setHybrid(boolean hybrid) {
        this.isHybridRule = hybrid;
    }

}
