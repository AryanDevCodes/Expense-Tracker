package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.entity.ApproverConfig;
import com.expenses.expensetracker.entity.ApprovalRule;
import com.expenses.expensetracker.repository.ApproverConfigRepository;
import com.expenses.expensetracker.repository.ApprovalRuleRepository;
import com.expenses.expensetracker.repository.UserRepository;
import com.expenses.expensetracker.service.ApproverConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ApproverConfigServiceImpl implements ApproverConfigService {

    @Autowired
    private ApproverConfigRepository approverConfigRepository;

    @Autowired
    private ApprovalRuleRepository approvalRuleRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ApproverConfig createApproverConfig(ApproverConfig config) {
        validateSequence(config);
        return approverConfigRepository.save(config);
    }

    @Override
    public void updateSequence(Long configId, int newSequence) {
        ApproverConfig config = approverConfigRepository.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Approver config not found"));

        config.setSequence(newSequence);
        validateSequence(config);
        approverConfigRepository.save(config);
    }

    @Override
    public List<ApproverConfig> getApproversForRule(Long ruleId) {
        return approverConfigRepository.findByApprovalRuleIdOrderBySequence(ruleId);
    }

    @Override
    public void removeApproverFromRule(Long ruleId, Long approverId) {
        approverConfigRepository.deleteByApprovalRuleIdAndApproverId(ruleId, approverId);
    }

    @Override
    public void setCFOApprover(Long ruleId, Long approverId) {
        ApprovalRule rule = approvalRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Approval rule not found"));

        ApproverConfig config = new ApproverConfig();
        config.setApprovalRule(rule);
        config.setApprover(userRepository.findById(approverId)
            .orElseThrow(() -> new IllegalArgumentException("Approver not found")));
        config.setCfoStep(true);

        approverConfigRepository.save(config);
    }

    @Override
    public void setRequiredPercentage(Long ruleId, int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }

        ApprovalRule rule = approvalRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Approval rule not found"));

        rule.setRequiredPercentage(percentage);
        approvalRuleRepository.save(rule);
    }

    @Override
    public List<ApproverConfig> findAll() {
        return approverConfigRepository.findAll();
    }

    @Override
    public Optional<ApproverConfig> findById(Long configId) {
        return approverConfigRepository.findById(configId);
    }

    @Override
    public ApproverConfig updateApproverConfig(ApproverConfig config) {
        return approverConfigRepository.save(config);
    }

    @Override
    public void deleteApproverConfig(Long configId) {
        approverConfigRepository.deleteById(configId);
    }

    private void validateSequence(ApproverConfig config) {
        if (config.isManagerStep() && config.getSequence() != 1) {
            throw new IllegalStateException("Manager approval must be first in sequence");
        }
        if (config.isFinanceStep() && config.getSequence() < 2) {
            throw new IllegalStateException("Finance approval must come after manager");
        }
        if (config.isDirectorStep() && config.getSequence() < 3) {
            throw new IllegalStateException("Director approval must come after finance");
        }
    }
}
