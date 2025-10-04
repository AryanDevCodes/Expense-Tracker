package com.expenses.expensetracker.config;

import com.expenses.expensetracker.entity.*;
import com.expenses.expensetracker.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataLoader {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final ExpenseRepository expenseRepository;
    private final ApprovalRuleRepository approvalRuleRepository;
    private final ApproverConfigRepository approverConfigRepository;
    private final ApprovalStepRepository approvalStepRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    @Transactional
    public void loadData() {
        // Delete in order to avoid FK and collection issues
        approvalStepRepository.deleteAllInBatch();
        expenseRepository.deleteAllInBatch();
        approverConfigRepository.deleteAllInBatch();
        approvalRuleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        roleRepository.deleteAllInBatch();
        companyRepository.deleteAllInBatch();

        // Insert Company
        Company tata = new Company();
        tata.setName("Tata Group");
        tata.setCountry("India");
        tata.setDefaultCurrency("INR");
        tata.setRequireManagerApproval(true);
        tata.setAllowMultiCurrency(true);
        tata.setCreatedAt(LocalDateTime.now());
        tata.setCreatedBy("system");
        companyRepository.save(tata);

        // Insert Roles - Fix to match 3-role requirement
        Role adminRole = Role.createAdminRole();
        roleRepository.save(adminRole);

        Role managerRole = Role.createManagerRole();
        roleRepository.save(managerRole);

        Role employeeRole = Role.createEmployeeRole();
        roleRepository.save(employeeRole);

        // Insert Users - Updated to use correct roles
        User arjun = new User();
        arjun.setUsername("arjun");
        arjun.setEmail("arjun@tata.com");
        arjun.setPassword(passwordEncoder.encode("password123"));
        arjun.setCompany(tata);
        arjun.setRoles(Set.of(adminRole));
        userRepository.save(arjun);

        User manager = new User();
        manager.setUsername("manager");
        manager.setEmail("manager@tata.com");
        manager.setPassword(passwordEncoder.encode("password123"));
        manager.setCompany(tata);
        manager.setRoles(Set.of(managerRole));
        userRepository.save(manager);

        User priya = new User();
        priya.setUsername("priya");
        priya.setEmail("priya@tata.com");
        priya.setPassword(passwordEncoder.encode("password123"));
        priya.setCompany(tata);
        priya.setManager(manager); // Assign manager to employee
        priya.setRoles(Set.of(employeeRole));
        userRepository.save(priya);

        User rahul = new User();
        rahul.setUsername("rahul");
        rahul.setEmail("rahul@tata.com");
        rahul.setPassword(passwordEncoder.encode("password123"));
        rahul.setCompany(tata);
        rahul.setManager(manager); // Assign manager to employee
        rahul.setRoles(Set.of(employeeRole));
        userRepository.save(rahul);

        // Insert ApprovalRule
        ApprovalRule rule = new ApprovalRule();
        rule.setCompany(tata);
        rule.setName("Default Rule");
        rule.setMinAmount(BigDecimal.valueOf(1000));
        rule.setMaxAmount(BigDecimal.valueOf(100000));
        rule.setRequiresManagerFirst(true);
        rule.setRequiredPercentage(60);
        rule.setCfoApprover(arjun);
        rule.setHybridRule(false);
        rule.setPercentageOrCfo(true);
        approvalRuleRepository.save(rule);

        // Insert ApproverConfig
        ApproverConfig config = new ApproverConfig();
        config.setApprovalRule(rule);
        config.setApprover(arjun);
        config.setSequence(1);
        config.setManagerStep(true);
        approverConfigRepository.save(config);

        // Insert Expense
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(5000));
        expense.setCurrency("INR");
        expense.setBaseCurrencyAmount(BigDecimal.valueOf(5000));
        expense.setExchangeRate(BigDecimal.ONE);
        expense.setCategory("Travel");
        expense.setDescription("Client meeting in Mumbai");
        expense.setDate(LocalDate.now());
        expense.setReceiptUrl("/receipts/receipt1.jpg");
        expense.setSubmitter(priya);
        expense.setStatus(ExpenseStatus.SUBMITTED);
        expense.setSubmittedAt(LocalDateTime.now());
        expense.setCompany(tata);
        expenseRepository.save(expense);

        // Insert ApprovalStep
        ApprovalStep step = new ApprovalStep();
        step.setExpense(expense);
        step.setApprover(arjun);
        step.setSequence(1);
        step.setStatus(ApprovalStep.ApprovalStepStatus.PENDING);
        step.setComments("Please review");
        step.setActionDate(LocalDateTime.now());
        step.setReminderSent(false);
        approvalStepRepository.save(step);
    }
}
