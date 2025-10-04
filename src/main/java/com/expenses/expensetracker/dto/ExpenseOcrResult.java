package com.expenses.expensetracker.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseOcrResult {
    private BigDecimal amount;
    private String currency;
    private LocalDate date;
    private String description;
    private String merchantName;
    private String expenseType;
    private String[] expenseLines;
    private boolean success;
    private String errorMessage;
}
