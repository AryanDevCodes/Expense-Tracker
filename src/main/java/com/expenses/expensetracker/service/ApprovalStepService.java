package com.expenses.expensetracker.service;

import com.expenses.expensetracker.entity.ApprovalStep;
import com.expenses.expensetracker.entity.Expense;
import java.util.List;
import java.util.Optional;

public interface ApprovalStepService {
    // Core approval workflow methods
    ApprovalStep createStepForExpense(Expense expense, Long approverId, int sequence);
    List<ApprovalStep> getPendingApprovalsForUser(Long approverId);

    // Required for Manager/Admin role
    void approveStep(Long stepId, String comments);
    void rejectStep(Long stepId, String comments);

    // For percentage rule calculation
    int calculateApprovalPercentage(Long expenseId);

    // For checking approval status
    boolean isStepComplete(Long expenseId, Long approverId);
    List<ApprovalStep> getStepsForExpense(Long expenseId);

    Optional<ApprovalStep> findById(Long stepId);
    ApprovalStep createApprovalStep(ApprovalStep step);
    ApprovalStep updateApprovalStep(ApprovalStep step);
    void deleteApprovalStep(Long stepId);
    List<ApprovalStep> findAll();
}
