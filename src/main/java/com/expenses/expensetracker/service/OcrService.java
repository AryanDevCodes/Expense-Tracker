package com.expenses.expensetracker.service;

import com.expenses.expensetracker.dto.ExpenseOcrResult;

public interface OcrService {
    /**
     * Process a receipt image and extract expense details:
     * - Amount
     * - Date
     * - Description
     * - Expense type
     * - Restaurant/merchant name
     * - Individual expense lines
     */
    ExpenseOcrResult processReceipt(byte[] receiptImage);
}
