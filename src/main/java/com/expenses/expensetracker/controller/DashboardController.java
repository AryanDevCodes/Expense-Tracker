package com.expenses.expensetracker.controller;

import java.util.List;
import com.expenses.expensetracker.entity.User;
import com.expenses.expensetracker.entity.Expense;
import com.expenses.expensetracker.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ApprovalWorkflowService workflowService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }

            Long userId = getUserId(userDetails);
            if (userId == null) {
                model.addAttribute("error", "User not found");
                return "error";
            }

            // Add role-specific dashboard data - Only 3 roles as per requirements
            if (userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return showAdminDashboard(model);
            } else if (userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"))) {
                return showManagerDashboard(userId, model);
            } else {
                // Default to Employee role
                return showEmployeeDashboard(userId, model);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "error";
        }
    }

    private String showManagerDashboard(Long managerId, Model model) {
        try {
            // Use cached methods for faster loading
            List<Expense> pendingApprovals = workflowService.getPendingApprovalsForUser(managerId);
            List<Expense> teamExpenses = workflowService.getTeamExpenses(managerId);

            model.addAttribute("view", "manager");
            model.addAttribute("pendingCount", pendingApprovals.size());
            model.addAttribute("teamExpenseCount", teamExpenses.size());
            model.addAttribute("pendingApprovals",
                pendingApprovals.subList(0, Math.min(5, pendingApprovals.size()))); // Show only 5 recent

            // Add manager-specific metrics for expense approval
            java.math.BigDecimal totalPendingAmount = pendingApprovals.stream()
                .map(Expense::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            model.addAttribute("totalPendingAmount", totalPendingAmount);

            long highValueExpenses = teamExpenses.stream()
                .filter(expense -> expense.getAmount().compareTo(new java.math.BigDecimal("1000")) > 0)
                .count();
            model.addAttribute("highValueExpenses", highValueExpenses);

            return "dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading manager dashboard");
            return "error";
        }
    }

    private String showEmployeeDashboard(Long employeeId, Model model) {
        try {
            // Use the interface methods directly - no casting needed
            Long expenseCount = expenseService.getExpenseCount(employeeId);
            List<Expense> recentExpenses = expenseService.getRecentExpenses(employeeId);

            model.addAttribute("view", "employee");
            model.addAttribute("myExpenseCount", expenseCount);
            model.addAttribute("recentExpenses", recentExpenses);

            // Add employee-specific metrics
            java.math.BigDecimal totalAmount = recentExpenses.stream()
                .map(Expense::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            model.addAttribute("totalExpenseAmount", totalAmount);

            long pendingCount = recentExpenses.stream()
                .filter(expense -> expense.getStatus().name().contains("PENDING"))
                .count();
            model.addAttribute("myPendingCount", pendingCount);

            long approvedCount = recentExpenses.stream()
                .filter(expense -> expense.getStatus().name().equals("APPROVED"))
                .count();
            model.addAttribute("myApprovedCount", approvedCount);

            return "dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading employee dashboard");
            return "error";
        }
    }

    private String showAdminDashboard(Model model) {
        try {
            model.addAttribute("view", "admin");

            // Load all pending approvals and expenses for admin
            List<Expense> allPendingApprovals = workflowService.getAllPendingApprovals();
            List<Expense> allExpenses = expenseService.findAll();
            List<User> allUsers = userService.findAll();

            // Set the correct variable names that match the admin dashboard template
            model.addAttribute("totalUsers", allUsers.size());
            model.addAttribute("totalExpenses", allExpenses.size());
            model.addAttribute("pendingApprovals", allPendingApprovals.size()); // This should be a count, not a list
            model.addAttribute("totalCompanies", 1);

            // Add recent expenses for the admin dashboard table
            model.addAttribute("recentExpenses",
                allExpenses.stream()
                    .sorted((e1, e2) -> e2.getSubmittedAt().compareTo(e1.getSubmittedAt()))
                    .limit(10)
                    .toList());

            // Add pending approvals list for admin review (separate from count)
            model.addAttribute("pendingApprovalsList",
                allPendingApprovals.stream().limit(10).toList());

            // Add additional metrics
            long approvedThisMonth = allExpenses.stream()
                .filter(expense -> expense.getStatus().name().equals("APPROVED"))
                .count();
            model.addAttribute("approvedThisMonth", approvedThisMonth);

            return "dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading admin dashboard: " + e.getMessage());
            return "error";
        }
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails != null) {
            String username = userDetails.getUsername();
            User currentUser = userService.findByUsername(username);
            if (currentUser != null) {
                return currentUser.getId();
            }
        }
        return null;
    }
}
