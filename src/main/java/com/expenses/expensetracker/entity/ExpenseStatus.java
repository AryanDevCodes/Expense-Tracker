package com.expenses.expensetracker.entity;

public enum ExpenseStatus {
    // Initial states
    DRAFT("Draft"),
    SUBMITTED("Submitted"),

    // Core approval states as per requirements
    PENDING_MANAGER("Pending Manager Approval"),
    PENDING_FINANCE("Pending Finance Approval"),
    PENDING_DIRECTOR("Pending Director Approval"),

    // Percentage-based states
    PARTIALLY_APPROVED("Partially Approved"),    // For tracking progress towards 60% approval
    CFO_APPROVED("CFO Approved"),               // For specific approver rule

    // Additional workflow states
    PENDING_ADDITIONAL_INFO("Additional Info Required"),

    // Final states
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    ExpenseStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminalState() {
        return this == APPROVED || this == REJECTED;
    }

    public boolean requiresAction() {
        return this == PENDING_MANAGER ||
               this == PENDING_FINANCE ||
               this == PENDING_DIRECTOR ||
               this == PENDING_ADDITIONAL_INFO;
    }

    public boolean canBeEdited() {
        return this == DRAFT ||
               this == PENDING_ADDITIONAL_INFO;
    }
}
