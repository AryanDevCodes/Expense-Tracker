package com.expenses.expensetracker.repository;

import com.expenses.expensetracker.entity.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    List<ApprovalStep> findByApproverIdAndStatus(Long approverId, ApprovalStep.ApprovalStepStatus status);
    List<ApprovalStep> findByExpenseIdOrderBySequence(Long expenseId);
    Optional<ApprovalStep> findByExpenseIdAndApproverId(Long expenseId, Long approverId);

    // Fix: Add the missing method that ExpenseRepository is trying to use
    @Query("SELECT s.expense FROM ApprovalStep s WHERE s.approver.id = :approverId AND s.status = 'PENDING'")
    List<com.expenses.expensetracker.entity.Expense> findExpensesByCurrentApproverId(@Param("approverId") Long approverId);

    // Additional methods needed by services
    @Query("SELECT COUNT(s) FROM ApprovalStep s WHERE s.expense.id = :expenseId")
    long countByExpenseId(@Param("expenseId") Long expenseId);

    @Query("SELECT COUNT(s) FROM ApprovalStep s WHERE s.expense.id = :expenseId AND s.status = :status")
    long countByExpenseIdAndStatus(@Param("expenseId") Long expenseId, @Param("status") ApprovalStep.ApprovalStepStatus status);

    boolean existsByExpenseIdAndStatus(Long expenseId, ApprovalStep.ApprovalStepStatus status);

    // Find current pending step for an expense
    @Query("SELECT s FROM ApprovalStep s WHERE s.expense.id = :expenseId AND s.status = 'PENDING' ORDER BY s.sequence ASC")
    Optional<ApprovalStep> findCurrentPendingStep(@Param("expenseId") Long expenseId);
}
