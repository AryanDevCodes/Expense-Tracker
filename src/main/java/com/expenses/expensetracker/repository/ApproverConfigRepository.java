package com.expenses.expensetracker.repository;

import com.expenses.expensetracker.entity.ApproverConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApproverConfigRepository extends JpaRepository<ApproverConfig, Long> {
    List<ApproverConfig> findByApprovalRuleIdOrderBySequence(Long ruleId);

    @Modifying
    @Query("DELETE FROM ApproverConfig ac WHERE ac.approvalRule.id = :ruleId AND ac.approver.id = :approverId")
    void deleteByApprovalRuleIdAndApproverId(Long ruleId, Long approverId);

    boolean existsByApprovalRuleIdAndApproverId(Long ruleId, Long approverId);
}
