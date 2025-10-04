package com.expenses.expensetracker.repository;

import com.expenses.expensetracker.entity.Expense;
import com.expenses.expensetracker.entity.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    // For employee role
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.submitter LEFT JOIN FETCH e.company WHERE e.submitter.id = :submitterId ORDER BY e.submittedAt DESC")
    List<Expense> findBySubmitterId(@Param("submitterId") Long submitterId);

    // For manager/approver role
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.approvalSteps s LEFT JOIN FETCH s.approver WHERE s.approver.id = :approverId AND s.status = 'PENDING' ORDER BY e.submittedAt ASC")
    List<Expense> findByCurrentApproverId(@Param("approverId") Long approverId);

    // For status-based queries
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.submitter WHERE e.submitter.id = :submitterId AND e.status = :status ORDER BY e.submittedAt DESC")
    List<Expense> findBySubmitterIdAndStatus(@Param("submitterId") Long submitterId, @Param("status") ExpenseStatus status);

    // For team expenses (manager role)
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.submitter LEFT JOIN FETCH e.company WHERE e.submitter.manager.id = :managerId ORDER BY e.submittedAt DESC")
    List<Expense> findTeamExpenses(@Param("managerId") Long managerId);

    // For admin reports - get expenses by status list with optimized fetching
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.submitter WHERE e.status IN :statuses ORDER BY e.submittedAt DESC")
    List<Expense> findByStatusIn(@Param("statuses") List<ExpenseStatus> statuses);

    // Performance optimized queries
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.submitter LEFT JOIN FETCH e.approvalSteps WHERE e.submitter.id = :submitterId ORDER BY e.submittedAt DESC LIMIT 10")
    List<Expense> findRecentExpensesBySubmitter(@Param("submitterId") Long submitterId);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.submitter.id = :submitterId")
    Long countBySubmitterId(@Param("submitterId") Long submitterId);
}
