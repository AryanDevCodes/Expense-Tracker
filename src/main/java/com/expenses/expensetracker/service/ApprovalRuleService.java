package com.expenses.expensetracker.service;

import com.expenses.expensetracker.entity.ApprovalRule;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ApprovalRuleService {
    // Core rule management
    ApprovalRule createRule(ApprovalRule rule);
    void updateRule(Long ruleId, int requiredPercentage, Long cfoApproverId);
    void deleteRule(Long ruleId);

    // CRUD operations
    List<ApprovalRule> findAll();
    Optional<ApprovalRule> findById(Long ruleId);
    ApprovalRule createApprovalRule(ApprovalRule rule);
    ApprovalRule updateApprovalRule(ApprovalRule rule);
    void deleteApprovalRule(Long ruleId);

    // Rule application methods
    ApprovalRule findApplicableRule(Long companyId, BigDecimal amount);
    boolean isPercentageRuleMet(Long ruleId, Long expenseId);

    // Admin operations
    List<ApprovalRule> getRulesForCompany(Long companyId);
    void setRuleSequence(Long ruleId, List<Long> approverIds);
}
