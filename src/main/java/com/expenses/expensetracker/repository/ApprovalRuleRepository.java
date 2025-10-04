package com.expenses.expensetracker.repository;

import com.expenses.expensetracker.entity.ApprovalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ApprovalRuleRepository extends JpaRepository<ApprovalRule, Long> {
    List<ApprovalRule> findByCompanyId(Long companyId);

    @Query("SELECT r FROM ApprovalRule r WHERE r.company.id = :companyId " +
           "AND (:amount BETWEEN r.minAmount AND r.maxAmount OR r.minAmount IS NULL) " +
           "ORDER BY r.minAmount ASC NULLS LAST LIMIT 1")
    Optional<ApprovalRule> findFirstByCompanyIdAndAmountRange(
        @Param("companyId") Long companyId,
        @Param("amount") BigDecimal amount
    );
}
