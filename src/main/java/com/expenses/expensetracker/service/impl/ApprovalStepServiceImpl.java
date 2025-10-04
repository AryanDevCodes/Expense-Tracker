package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.entity.ApprovalStep;
import com.expenses.expensetracker.entity.Expense;
import com.expenses.expensetracker.repository.ApprovalStepRepository;
import com.expenses.expensetracker.repository.UserRepository;
import com.expenses.expensetracker.service.ApprovalStepService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ApprovalStepServiceImpl implements ApprovalStepService {

    @Autowired
    private ApprovalStepRepository approvalStepRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ApprovalStep createStepForExpense(Expense expense, Long approverId, int sequence) {
        ApprovalStep step = new ApprovalStep();
        step.setExpense(expense);
        step.setApprover(userRepository.findById(approverId)
            .orElseThrow(() -> new IllegalArgumentException("Approver not found")));
        step.setSequence(sequence);
        return approvalStepRepository.save(step);
    }

    @Override
    public List<ApprovalStep> getPendingApprovalsForUser(Long approverId) {
        return approvalStepRepository.findByApproverIdAndStatus(
            approverId,
            ApprovalStep.ApprovalStepStatus.PENDING
        );
    }

    @Override
    public void approveStep(Long stepId, String comments) {
        ApprovalStep step = approvalStepRepository.findById(stepId)
            .orElseThrow(() -> new IllegalArgumentException("Approval step not found"));
        step.approve(comments);
        approvalStepRepository.save(step);
    }

    @Override
    public void rejectStep(Long stepId, String comments) {
        ApprovalStep step = approvalStepRepository.findById(stepId)
            .orElseThrow(() -> new IllegalArgumentException("Approval step not found"));
        step.reject(comments);
        approvalStepRepository.save(step);
    }

    @Override
    public int calculateApprovalPercentage(Long expenseId) {
        List<ApprovalStep> steps = getStepsForExpense(expenseId);
        if (steps.isEmpty()) return 0;

        long approvedCount = steps.stream()
            .filter(step -> step.getStatus() == ApprovalStep.ApprovalStepStatus.APPROVED)
            .count();

        return (int) ((approvedCount * 100.0) / steps.size());
    }

    @Override
    public boolean isStepComplete(Long expenseId, Long approverId) {
        return approvalStepRepository.findByExpenseIdAndApproverId(expenseId, approverId)
            .map(ApprovalStep::isCompleted)
            .orElse(false);
    }

    @Override
    public List<ApprovalStep> getStepsForExpense(Long expenseId) {
        return approvalStepRepository.findByExpenseIdOrderBySequence(expenseId);
    }

    @Override
    public Optional<ApprovalStep> findById(Long stepId) {
        return approvalStepRepository.findById(stepId);
    }

    @Override
    public ApprovalStep createApprovalStep(ApprovalStep step) {
        return approvalStepRepository.save(step);
    }

    @Override
    public ApprovalStep updateApprovalStep(ApprovalStep step) {
        return approvalStepRepository.save(step);
    }

    @Override
    public void deleteApprovalStep(Long stepId) {
        approvalStepRepository.deleteById(stepId);
    }

    @Override
    public List<ApprovalStep> findAll() {
        return approvalStepRepository.findAll();
    }
}
