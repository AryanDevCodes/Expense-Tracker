package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.entity.ApprovalRule;
import com.expenses.expensetracker.entity.ApprovalStep;
import com.expenses.expensetracker.entity.ApproverConfig;
import com.expenses.expensetracker.repository.ApprovalRuleRepository;
import com.expenses.expensetracker.repository.ApprovalStepRepository;
import com.expenses.expensetracker.repository.UserRepository;
import com.expenses.expensetracker.service.ApprovalRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ApprovalRuleServiceImpl implements ApprovalRuleService {

    @Autowired
    private ApprovalRuleRepository approvalRuleRepository;

    @Autowired
    private ApprovalStepRepository approvalStepRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ApprovalRule createRule(ApprovalRule rule) {
        validateRule(rule);
        return approvalRuleRepository.save(rule);
    }

    @Override
    public void updateRule(Long ruleId, int requiredPercentage, Long cfoApproverId) {
        ApprovalRule rule = approvalRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found"));

        if (requiredPercentage > 0) {
            if (requiredPercentage > 100) {
                throw new IllegalArgumentException("Percentage must be between 0 and 100");
            }
            rule.setRequiredPercentage(requiredPercentage);
        }

        if (cfoApproverId != null) {
            rule.setCfoApprover(userRepository.findById(cfoApproverId)
                .orElseThrow(() -> new IllegalArgumentException("CFO approver not found")));
        }

        approvalRuleRepository.save(rule);
    }

    @Override
    public ApprovalRule findApplicableRule(Long companyId, BigDecimal amount) {
        return approvalRuleRepository.findFirstByCompanyIdAndAmountRange(companyId, amount)
            .orElseThrow(() -> new IllegalStateException("No applicable rule found"));
    }

    @Override
    public boolean isPercentageRuleMet(Long ruleId, Long expenseId) {
        ApprovalRule rule = approvalRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found"));

        if (rule.getRequiredPercentage() == null) {
            return false;
        }

        long totalSteps = approvalStepRepository.countByExpenseId(expenseId);
        if (totalSteps == 0) return false;

        long approvedSteps = approvalStepRepository.countByExpenseIdAndStatus(
            expenseId,
            ApprovalStep.ApprovalStepStatus.APPROVED
        );

        return (approvedSteps * 100.0 / totalSteps) >= rule.getRequiredPercentage();
    }

    @Override
    public List<ApprovalRule> getRulesForCompany(Long companyId) {
        return approvalRuleRepository.findByCompanyId(companyId);
    }

    @Override
    public void setRuleSequence(Long ruleId, List<Long> approverIds) {
        ApprovalRule rule = approvalRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found"));

        // Clear existing approvers
        rule.getApprovers().clear();

        // Add approvers in sequence
        for (int i = 0; i < approverIds.size(); i++) {
            ApproverConfig config = new ApproverConfig();
            config.setApprovalRule(rule);
            config.setApprover(userRepository.findById(approverIds.get(i))
                .orElseThrow(() -> new IllegalArgumentException("Approver not found")));
            config.setSequence(i + 1);
            rule.getApprovers().add(config);
        }

        approvalRuleRepository.save(rule);
    }

    @Override
    public void deleteRule(Long ruleId) {
        if (!approvalRuleRepository.existsById(ruleId)) {
            throw new IllegalArgumentException("Rule not found");
        }
        approvalRuleRepository.deleteById(ruleId);
    }

    @Override
    public List<ApprovalRule> findAll() {
        return approvalRuleRepository.findAll();
    }

    @Override
    public Optional<ApprovalRule> findById(Long ruleId) {
        return approvalRuleRepository.findById(ruleId);
    }

    @Override
    public ApprovalRule createApprovalRule(ApprovalRule rule) {
        return createRule(rule);
    }

    @Override
    public ApprovalRule updateApprovalRule(ApprovalRule rule) {
        return approvalRuleRepository.save(rule);
    }

    @Override
    public void deleteApprovalRule(Long ruleId) {
        deleteRule(ruleId);
    }

    private void validateRule(ApprovalRule rule) {
        if (rule.getRequiredPercentage() != null) {
            if (rule.getRequiredPercentage() < 0 || rule.getRequiredPercentage() > 100) {
                throw new IllegalArgumentException("Percentage must be between 0 and 100");
            }
        }

        if (rule.isHybridRule() && rule.getCfoApprover() == null) {
            throw new IllegalArgumentException("Hybrid rule must have a CFO approver");
        }
    }
}
