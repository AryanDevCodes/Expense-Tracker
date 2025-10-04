package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.entity.*;
import com.expenses.expensetracker.repository.*;
import com.expenses.expensetracker.service.ApprovalWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Service
@Transactional
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ApprovalStepRepository approvalStepRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void initiateWorkflow(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        User submitter = expense.getSubmitter();

        // Check the role of the submitter to determine approval flow
        if (submitter.hasRole("MANAGER")) {
            // Manager submissions go directly to admin/finance for approval
            User adminApprover = getFinanceApprover();
            createApprovalStep(expense, adminApprover, 1);
            expense.setStatus(ExpenseStatus.PENDING_FINANCE);
        } else if (submitter.hasManager()) {
            // Regular employee submissions go to their manager first
            User manager = submitter.getManager();
            createApprovalStep(expense, manager, 1);
            expense.setStatus(ExpenseStatus.PENDING_MANAGER);
        } else {
            // Employees without managers go directly to finance
            User financeApprover = getFinanceApprover();
            createApprovalStep(expense, financeApprover, 1);
            expense.setStatus(ExpenseStatus.PENDING_FINANCE);
        }

        expenseRepository.save(expense);
    }

    @Override
    public void initiateManagerExpenseWorkflow(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        User submitter = expense.getSubmitter();

        // Verify the submitter is actually a manager
        if (!submitter.hasRole("MANAGER")) {
            throw new IllegalStateException("Only managers can use manager expense workflow");
        }

        // For manager expenses, determine approval path based on amount
        if (expense.getAmount().compareTo(new BigDecimal("25000")) > 0) {
            // High-value manager expenses go directly to admin/director
            User directorApprover = getDirectorApprover();
            createApprovalStep(expense, directorApprover, 1);
            expense.setStatus(ExpenseStatus.PENDING_DIRECTOR);
        } else {
            // Regular manager expenses go to admin/finance
            User adminApprover = getFinanceApprover();
            createApprovalStep(expense, adminApprover, 1);
            expense.setStatus(ExpenseStatus.PENDING_FINANCE);
        }

        expenseRepository.save(expense);
    }

    @Override
    public void processManagerApproval(Long expenseId, Long managerId, String comments) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        validateApprover(expense, managerId, "MANAGER");

        // Approve current step
        ApprovalStep currentStep = getCurrentStep(expense);
        currentStep.approve(comments);
        approvalStepRepository.save(currentStep);

        // Move to finance approval
        User financeApprover = getFinanceApprover();
        createApprovalStep(expense, financeApprover, 2);
        expense.setStatus(ExpenseStatus.PENDING_FINANCE);
        expenseRepository.save(expense);
    }

    @Override
    public void processFinanceApproval(Long expenseId, Long financeId, String comments) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        validateApprover(expense, financeId, "FINANCE");

        ApprovalStep currentStep = getCurrentStep(expense);
        currentStep.approve(comments);
        approvalStepRepository.save(currentStep);

        if (needsDirectorApproval(expense)) {
            User director = getDirectorApprover();
            createApprovalStep(expense, director, 3);
            expense.setStatus(ExpenseStatus.PENDING_DIRECTOR);
        } else if (checkPercentageApproval(expenseId)) {
            expense.setStatus(ExpenseStatus.APPROVED);
        }

        expenseRepository.save(expense);
    }

    @Override
    public void processDirectorApproval(Long expenseId, Long directorId, String comments) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        validateApprover(expense, directorId, "DIRECTOR");

        ApprovalStep currentStep = getCurrentStep(expense);
        currentStep.approve(comments);
        approvalStepRepository.save(currentStep);

        expense.setStatus(ExpenseStatus.APPROVED);
        expenseRepository.save(expense);
    }

    @Override
    public boolean processCFOApproval(Long expenseId, Long cfoId, String comments) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        User cfo = userRepository.findById(cfoId)
            .orElseThrow(() -> new IllegalArgumentException("CFO not found"));

        if (!isCFO(cfo)) {
            throw new IllegalStateException("Approver is not CFO");
        }

        ApprovalStep step = new ApprovalStep();
        step.setExpense(expense);
        step.setApprover(cfo);
        step.setSequence(1);
        step.approve(comments);
        approvalStepRepository.save(step);

        expense.setStatus(ExpenseStatus.CFO_APPROVED);
        expenseRepository.save(expense);
        return true;
    }

    @Override
    public void rejectExpense(Long expenseId, Long approverId, String reason) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        ApprovalStep currentStep = getCurrentStep(expense);
        if (!currentStep.getApprover().getId().equals(approverId)) {
            throw new IllegalStateException("Not authorized to reject this expense");
        }

        currentStep.reject(reason);
        approvalStepRepository.save(currentStep);

        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setRejectionReason(reason);
        expenseRepository.save(expense);
    }

    @Override
    public boolean checkPercentageApproval(Long expenseId) {
        List<ApprovalStep> steps = approvalStepRepository.findByExpenseIdOrderBySequence(expenseId);
        if (steps.isEmpty()) return false;

        long approvedCount = steps.stream()
            .filter(step -> step.getStatus() == ApprovalStep.ApprovalStepStatus.APPROVED)
            .count();

        return (approvedCount * 100.0 / steps.size()) >= 60; // 60% requirement
    }

    @Override
    public List<Expense> getPendingApprovalsForUser(Long approverId) {
        return approvalStepRepository.findExpensesByCurrentApproverId(approverId);
    }

    @Override
    public List<Expense> getTeamExpenses(Long managerId) {
        return expenseRepository.findTeamExpenses(managerId);
    }

    @Override
    public int calculateApprovalPercentage(Long expenseId) {
        List<ApprovalStep> steps = approvalStepRepository.findByExpenseIdOrderBySequence(expenseId);
        if (steps.isEmpty()) return 0;

        long approvedCount = steps.stream()
            .filter(step -> step.getStatus() == ApprovalStep.ApprovalStepStatus.APPROVED)
            .count();

        return (int) ((approvedCount * 100.0) / steps.size());
    }

    @Override
    public boolean isApprovalComplete(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        return expense.getStatus() == ExpenseStatus.APPROVED ||
               expense.getStatus() == ExpenseStatus.CFO_APPROVED ||
               expense.getStatus() == ExpenseStatus.REJECTED;
    }

    @Override
    public List<Expense> getAllPendingApprovals() {
        return expenseRepository.findByStatusIn(List.of(
            ExpenseStatus.SUBMITTED,
            ExpenseStatus.PENDING_MANAGER,
            ExpenseStatus.PENDING_FINANCE,
            ExpenseStatus.PENDING_DIRECTOR,
            ExpenseStatus.PENDING_ADDITIONAL_INFO
        ));
    }

    // Helper methods
    private ApprovalStep getCurrentStep(Expense expense) {
        return expense.getApprovalSteps().stream()
            .filter(step -> step.getStatus() == ApprovalStep.ApprovalStepStatus.PENDING)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No pending approval step found"));
    }

    private void createApprovalStep(Expense expense, User approver, int sequence) {
        ApprovalStep step = new ApprovalStep();
        step.setExpense(expense);
        step.setApprover(approver);
        step.setSequence(sequence);
        step.setStatus(ApprovalStep.ApprovalStepStatus.PENDING);
        approvalStepRepository.save(step);
    }

    private User getFinanceApprover() {
        // Get first ADMIN user to act as finance approver
        return userRepository.findAll().stream()
            .filter(user -> user.hasRole("ADMIN"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No admin user found to act as finance approver"));
    }

    private User getDirectorApprover() {
        // Get first ADMIN user to act as director approver
        return userRepository.findAll().stream()
            .filter(user -> user.hasRole("ADMIN"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No admin user found to act as director approver"));
    }

    private boolean isCFO(User user) {
        return user.hasRole("ADMIN"); // Admin users can act as CFO
    }

    private void validateApprover(Expense expense, Long approverId, String expectedRole) {
        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new IllegalArgumentException("Approver not found"));

        // Allow ADMIN users to approve any step, or users with the expected role
        boolean hasPermission = approver.hasRole("ADMIN") ||
                               (expectedRole.equals("MANAGER") && approver.hasRole("USER")) ||
                               approver.hasRole(expectedRole);

        if (!hasPermission) {
            throw new IllegalStateException("Approver does not have required permissions");
        }

        ApprovalStep currentStep = getCurrentStep(expense);
        if (!currentStep.getApprover().getId().equals(approverId)) {
            throw new IllegalStateException("Not authorized to approve this step");
        }
    }

    private boolean needsDirectorApproval(Expense expense) {
        // Director approval needed for high-value expenses (over ₹50,000)
        return expense.getAmount().compareTo(new BigDecimal("50000")) > 0;
    }

    @Override
    public void processAdminOverride(Long expenseId, Long adminId, String comments) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        if (!admin.hasRole("ADMIN")) {
            throw new IllegalStateException("Only admin users can override approvals");
        }

        // Create admin override approval step
        ApprovalStep overrideStep = new ApprovalStep();
        overrideStep.setExpense(expense);
        overrideStep.setApprover(admin);
        overrideStep.setSequence(999); // High sequence number for override
        overrideStep.approve(comments);
        approvalStepRepository.save(overrideStep);

        // Mark all pending steps as skipped
        List<ApprovalStep> pendingSteps = approvalStepRepository.findByExpenseIdOrderBySequence(expenseId)
            .stream()
            .filter(step -> step.getStatus() == ApprovalStep.ApprovalStepStatus.PENDING)
            .toList();

        for (ApprovalStep step : pendingSteps) {
            step.skip("Skipped due to admin override");
            approvalStepRepository.save(step);
        }

        // Directly approve the expense
        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setLastActionAt(LocalDateTime.now());
        expenseRepository.save(expense);
    }

    @Override
    public void escalateExpense(Long expenseId, Long managerId, String escalationReason) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        User manager = userRepository.findById(managerId)
            .orElseThrow(() -> new IllegalArgumentException("Manager not found"));

        if (!manager.hasRole("MANAGER") && !manager.hasRole("ADMIN")) {
            throw new IllegalStateException("Only managers can escalate expenses");
        }

        // Create escalation step - goes to admin/director
        User director = getDirectorApprover();
        createApprovalStep(expense, director, getCurrentMaxSequence(expenseId) + 1);

        // Update expense status
        expense.setStatus(ExpenseStatus.PENDING_DIRECTOR);
        expense.setLastActionAt(LocalDateTime.now());
        expenseRepository.save(expense);

        // Log escalation in approval step comments
        ApprovalStep escalationLog = new ApprovalStep();
        escalationLog.setExpense(expense);
        escalationLog.setApprover(manager);
        escalationLog.setSequence(getCurrentMaxSequence(expenseId) + 1);
        escalationLog.setComments("ESCALATED: " + escalationReason);
        escalationLog.setStatus(ApprovalStep.ApprovalStepStatus.APPROVED);
        escalationLog.setActionDate(LocalDateTime.now());
        approvalStepRepository.save(escalationLog);
    }

    private int getCurrentMaxSequence(Long expenseId) {
        return approvalStepRepository.findByExpenseIdOrderBySequence(expenseId)
            .stream()
            .mapToInt(ApprovalStep::getSequence)
            .max()
            .orElse(0);
    }
}
