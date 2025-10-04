package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.entity.Expense;
import com.expenses.expensetracker.entity.ExpenseStatus;
import com.expenses.expensetracker.dto.ExpenseOcrResult;
import com.expenses.expensetracker.repository.ExpenseRepository;
import com.expenses.expensetracker.service.ExpenseService;
import com.expenses.expensetracker.service.OcrService;
import com.expenses.expensetracker.service.CurrencyService;
import com.expenses.expensetracker.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.io.IOException;

@Service
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final OcrService ocrService;
    private final CurrencyService currencyService;
    private final FileStorageService fileStorageService;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository, OcrService ocrService,
                             CurrencyService currencyService, FileStorageService fileStorageService) {
        this.expenseRepository = expenseRepository;
        this.ocrService = ocrService;
        this.currencyService = currencyService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @CacheEvict(value = "userExpenses", key = "#expense.submitter.id")
    public Expense submitExpense(Expense expense) {
        // Validate expense
        if (expense.getAmount() == null || expense.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid expense amount");
        }

        // Set initial status and timestamps
        expense.setStatus(ExpenseStatus.SUBMITTED);
        expense.setSubmittedAt(java.time.LocalDateTime.now());
        expense.setLastActionAt(java.time.LocalDateTime.now());

        return expenseRepository.save(expense);
    }

    @Override
    public void uploadReceipt(Long expenseId, byte[] receipt) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        try {
            // Generate a unique filename for the receipt
            String fileName = fileStorageService.generateReceiptFileName(expenseId, "pdf");

            // Store the file and get the URL
            String receiptUrl = fileStorageService.storeFile(fileName, receipt);

            // Delete old receipt if it exists
            if (expense.getReceiptUrl() != null && !expense.getReceiptUrl().isEmpty()) {
                try {
                    // Extract filename from URL and delete old file
                    String oldFileName = expense.getReceiptUrl().substring(expense.getReceiptUrl().lastIndexOf("/") + 1);
                    fileStorageService.deleteFile(oldFileName);
                } catch (Exception e) {
                    // Log warning but don't fail the upload
                    System.err.println("Warning: Could not delete old receipt file: " + e.getMessage());
                }
            }

            // Update expense with new receipt URL
            expense.setReceiptUrl(receiptUrl);
            expenseRepository.save(expense);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store receipt file", e);
        }
    }

    @Override
    @Cacheable(value = "userExpenses", key = "#employeeId")
    public List<Expense> getEmployeeExpenses(Long employeeId) {
        return expenseRepository.findBySubmitterId(employeeId);
    }

    @Override
    @Cacheable(value = "teamExpenses", key = "#managerId")
    public List<Expense> getTeamExpenses(Long managerId) {
        return expenseRepository.findTeamExpenses(managerId);
    }

    @Override
    public Optional<Expense> findById(Long id) {
        return expenseRepository.findById(id);
    }

    @Override
    public BigDecimal convertToCompanyCurrency(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        String companyCurrency = expense.getSubmitter().getCompanyCurrency();
        if (expense.getCurrency().equals(companyCurrency)) {
            return expense.getAmount();
        }

        return currencyService.convertAmount(
            expense.getAmount(),
            expense.getCurrency(),
            companyCurrency
        );
    }

    @Override
    public List<Expense> findAll() {
        return expenseRepository.findAll();
    }

    // Add performance optimized method for dashboard
    @Override
    @Cacheable(value = "recentExpenses", key = "#employeeId")
    public List<Expense> getRecentExpenses(Long employeeId) {
        return expenseRepository.findRecentExpensesBySubmitter(employeeId);
    }

    @Override
    @Cacheable(value = "expenseCount", key = "#employeeId")
    public Long getExpenseCount(Long employeeId) {
        return expenseRepository.countBySubmitterId(employeeId);
    }
}
