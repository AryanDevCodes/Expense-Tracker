package com.expenses.expensetracker.service;

import com.expenses.expensetracker.entity.ApproverConfig;
import java.util.List;
import java.util.Optional;

public interface ApproverConfigService {
    // Core methods for managing approval sequences
    ApproverConfig createApproverConfig(ApproverConfig config);
    void updateSequence(Long configId, int newSequence);
    List<ApproverConfig> getApproversForRule(Long ruleId);
    void removeApproverFromRule(Long ruleId, Long approverId);

    // Methods for CFO and percentage-based rules
    void setCFOApprover(Long ruleId, Long approverId);
    void setRequiredPercentage(Long ruleId, int percentage);

    List<ApproverConfig> findAll();
    Optional<ApproverConfig> findById(Long configId);
    ApproverConfig updateApproverConfig(ApproverConfig config);
    void deleteApproverConfig(Long configId);
}
