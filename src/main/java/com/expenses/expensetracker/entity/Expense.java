package com.expenses.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses",
    indexes = {
        @Index(name = "idx_expense_submitter", columnList = "submitter_id"),
        @Index(name = "idx_expense_status", columnList = "status"),
        @Index(name = "idx_expense_date", columnList = "expense_date"),
        @Index(name = "idx_expense_amount_currency", columnList = "amount,currency")
    }
)
@Getter
@Setter
@EqualsAndHashCode(exclude = {"approvalSteps"})
@ToString(exclude = {"approvalSteps", "submitter", "company"})
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "base_currency_amount")
    private BigDecimal baseCurrencyAmount;

    @Column(name = "exchange_rate")
    private BigDecimal exchangeRate;

    @Column(nullable = false)
    private String category;

    @Column(length = 1000)
    private String description;

    @Column(name = "expense_date", nullable = false)
    private LocalDate date;

    @Column(name = "receipt_url")
    private String receiptUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id", nullable = false)
    private User submitter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseStatus status = ExpenseStatus.DRAFT;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence")
    private List<ApprovalStep> approvalSteps = new ArrayList<>();

    // Dates for tracking
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "last_action_at")
    private LocalDateTime lastActionAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Rejection details
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @PrePersist
    protected void onCreate() {
        if (this.status == ExpenseStatus.DRAFT) {
            this.lastActionAt = LocalDateTime.now();
        }
    }

    public void submit() {
        if (this.status != ExpenseStatus.DRAFT) {
            throw new IllegalStateException("Can only submit expenses in DRAFT status");
        }
        this.status = ExpenseStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.lastActionAt = LocalDateTime.now();
    }

    public void approve() {
        if (!canBeApproved()) {
            throw new IllegalStateException("Expense cannot be approved in current state: " + this.status);
        }
        this.status = ExpenseStatus.APPROVED;
        this.completedAt = LocalDateTime.now();
        this.lastActionAt = LocalDateTime.now();
    }

    public void approveByPercentage() {
        if (this.status != ExpenseStatus.PARTIALLY_APPROVED) {
            throw new IllegalStateException("Can only approve by percentage when partially approved");
        }
        this.status = ExpenseStatus.APPROVED;
        this.completedAt = LocalDateTime.now();
        this.lastActionAt = LocalDateTime.now();
    }

    public void approveByCfo() {
        if (!canBeApproved()) {
            throw new IllegalStateException("Expense cannot be CFO approved in current state: " + this.status);
        }
        this.status = ExpenseStatus.CFO_APPROVED;
        this.completedAt = LocalDateTime.now();
        this.lastActionAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        if (!canBeRejected()) {
            throw new IllegalStateException("Expense cannot be rejected in current state: " + this.status);
        }
        this.status = ExpenseStatus.REJECTED;
        this.rejectionReason = reason;
        this.completedAt = LocalDateTime.now();
        this.lastActionAt = LocalDateTime.now();
    }

    public void requestAdditionalInfo() {
        if (!isPending()) {
            throw new IllegalStateException("Can only request info for pending expenses");
        }
        this.status = ExpenseStatus.PENDING_ADDITIONAL_INFO;
        this.lastActionAt = LocalDateTime.now();
    }

    public void moveToNextApprover(User nextApprover) {
        if (!canMoveToNextApprover()) {
            throw new IllegalStateException("Cannot move to next approver in current state: " + this.status);
        }

        if (nextApprover.hasRole("MANAGER")) {
            this.status = ExpenseStatus.PENDING_MANAGER;
        } else if (nextApprover.hasRole("FINANCE")) {
            this.status = ExpenseStatus.PENDING_FINANCE;
        } else if (nextApprover.hasRole("DIRECTOR")) {
            this.status = ExpenseStatus.PENDING_DIRECTOR;
        }
        this.lastActionAt = LocalDateTime.now();
    }

    public void updatePartialApproval(int approvalPercentage) {
        if (approvalPercentage >= 60) {
            this.status = ExpenseStatus.PARTIALLY_APPROVED;
        }
        this.lastActionAt = LocalDateTime.now();
    }

    public boolean isEditable() {
        return this.status.canBeEdited();
    }

    public boolean isPending() {
        return this.status.requiresAction();
    }

    public boolean isCompleted() {
        return this.status.isTerminalState();
    }

    public boolean canBeApproved() {
        return isPending() || this.status == ExpenseStatus.PARTIALLY_APPROVED;
    }

    public boolean canBeRejected() {
        return isPending() || this.status == ExpenseStatus.PARTIALLY_APPROVED;
    }

    public boolean canMoveToNextApprover() {
        return this.status == ExpenseStatus.SUBMITTED ||
               this.status == ExpenseStatus.PENDING_MANAGER ||
               this.status == ExpenseStatus.PENDING_FINANCE;
    }

    public ApprovalStep getCurrentApprovalStep() {
        return this.approvalSteps.stream()
            .filter(step -> step.getStatus() == ApprovalStep.ApprovalStepStatus.PENDING)
            .findFirst()
            .orElse(null);
    }

    public List<ApprovalStep> getApprovedSteps() {
        return this.approvalSteps.stream()
            .filter(step -> step.getStatus() == ApprovalStep.ApprovalStepStatus.APPROVED)
            .toList();
    }
}
