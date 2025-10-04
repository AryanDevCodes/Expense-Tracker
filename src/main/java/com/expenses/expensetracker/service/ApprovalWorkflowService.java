package com.expenses.expensetracker.service;

import com.expenses.expensetracker.entity.Expense;
import java.util.List;

public interface ApprovalWorkflowService {
    // Core workflow methods
    void initiateWorkflow(Long expenseId);
    void processManagerApproval(Long expenseId, Long managerId, String comments);
    void processFinanceApproval(Long expenseId, Long financeId, String comments);
    void processDirectorApproval(Long expenseId, Long directorId, String comments);
    void rejectExpense(Long expenseId, Long approverId, String reason);

    // Manager-specific expense submission
    void initiateManagerExpenseWorkflow(Long expenseId);

    // Conditional approval methods
    boolean checkPercentageApproval(Long expenseId);
    boolean processCFOApproval(Long expenseId, Long cfoId, String comments);

    // View methods
    List<Expense> getPendingApprovalsForUser(Long approverId);
    List<Expense> getTeamExpenses(Long managerId);

    // Required for workflow progress
    int calculateApprovalPercentage(Long expenseId);
    boolean isApprovalComplete(Long expenseId);

    // Admin method to get all pending approvals across the system
    List<Expense> getAllPendingApprovals();

    void processAdminOverride(Long expenseId, Long adminId, String comments);
    void escalateExpense(Long expenseId, Long managerId, String escalationReason);
}
