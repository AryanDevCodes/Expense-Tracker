package com.expenses.expensetracker.service;

import com.expenses.expensetracker.entity.Expense;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface ExpenseService {
    // Core expense submission methods
    Expense submitExpense(Expense expense);
    void uploadReceipt(Long expenseId, byte[] receipt);

    // View methods for different roles
    List<Expense> getEmployeeExpenses(Long employeeId);
    List<Expense> getTeamExpenses(Long managerId);
    Optional<Expense> findById(Long id);

    // Required for currency conversion
    BigDecimal convertToCompanyCurrency(Long expenseId);

    // Admin method to get all expenses
    List<Expense> findAll();

    // Performance optimized methods
    List<Expense> getRecentExpenses(Long employeeId);
    Long getExpenseCount(Long employeeId);
}
