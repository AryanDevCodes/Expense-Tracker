package com.expenses.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_steps",
    indexes = {
        @Index(name = "idx_step_expense", columnList = "expense_id"),
        @Index(name = "idx_step_approver", columnList = "approver_id"),
        @Index(name = "idx_step_sequence", columnList = "sequence"),
        @Index(name = "idx_step_status", columnList = "status"),
        @Index(name = "idx_step_expense_status", columnList = "expense_id,status")
    }
)
@Getter
@Setter
@EqualsAndHashCode(exclude = {"expense", "approver"})
@ToString(exclude = {"expense", "approver"})
public class ApprovalStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Column(nullable = false)
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStepStatus status = ApprovalStepStatus.PENDING;

    @Column(length = 500)
    private String comments;

    @Column(name = "action_date")
    private LocalDateTime actionDate;

    @Column(name = "reminder_sent")
    private boolean reminderSent = false;

    @Column(name = "last_reminder_date")
    private LocalDateTime lastReminderDate;

    public enum ApprovalStepStatus {
        PENDING,
        APPROVED,
        REJECTED,
        SKIPPED        // For when CFO approval bypasses normal flow
    }

    // Action methods
    public void approve(String comments) {
        this.status = ApprovalStepStatus.APPROVED;
        this.comments = comments;
        this.actionDate = LocalDateTime.now();
    }

    public void reject(String comments) {
        this.status = ApprovalStepStatus.REJECTED;
        this.comments = comments;
        this.actionDate = LocalDateTime.now();
    }

    public void skip(String reason) {
        this.status = ApprovalStepStatus.SKIPPED;
        this.comments = reason;
        this.actionDate = LocalDateTime.now();
    }

    // Helper methods
    public boolean isPending() {
        return status == ApprovalStepStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == ApprovalStepStatus.APPROVED ||
               status == ApprovalStepStatus.REJECTED;
    }

    public boolean isManagerStep() {
        return approver.isManager() && approver.equals(expense.getSubmitter().getManager());
    }

    public boolean needsReminder() {
        if (!isPending()) return false;
        if (lastReminderDate == null) return true;
        return LocalDateTime.now().minusDays(1).isAfter(lastReminderDate);
    }
}
