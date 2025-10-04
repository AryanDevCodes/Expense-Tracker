package com.expenses.expensetracker.controller;

import com.expenses.expensetracker.entity.*;
import com.expenses.expensetracker.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ApprovalRuleService ruleService;

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ApprovalWorkflowService approvalWorkflowService;

    @Autowired
    private RoleService roleService;

    // User Management Routes
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "users/list";
    }

    @GetMapping("/users/new")
    public String showCreateUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("companies", companyService.findAll());
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("managers", userService.getUsersByRole("MANAGER"));
        return "users/form";
    }

    @PostMapping("/users")
    public String createUser(@ModelAttribute User user,
                           @RequestParam String roleName,
                           @RequestParam(required = false) Long managerId,
                           @RequestParam Long companyId,
                           RedirectAttributes redirectAttributes) {
        try {
            // Set company
            Company company = companyService.findById(companyId).orElseThrow();
            user.setCompany(company);

            // Assign role
            Role role = roleService.findByName(roleName).orElseThrow();
            user.getRoles().clear();
            user.getRoles().add(role);

            // Assign manager if creating an employee
            if ("EMPLOYEE".equals(roleName) && managerId != null) {
                User manager = userService.findById(managerId).orElseThrow();
                user.setManager(manager);
            }

            userService.createUser(user);
            redirectAttributes.addFlashAttribute("message", "User created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating user: " + e.getMessage());
            return "redirect:/admin/users/new";
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("companies", companyService.findAll());
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("managers", userService.getUsersByRole("MANAGER"));
        return "users/form";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id,
                           @ModelAttribute User userForm,
                           @RequestParam String roleName,
                           @RequestParam(required = false) Long managerId,
                           @RequestParam Long companyId,
                           RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id).orElseThrow();

            // Update basic info
            user.setEmail(userForm.getEmail());

            // Update company
            Company company = companyService.findById(companyId).orElseThrow();
            user.setCompany(company);

            // Update role
            Role role = roleService.findByName(roleName).orElseThrow();
            user.getRoles().clear();
            user.getRoles().add(role);

            // Update manager
            if ("EMPLOYEE".equals(roleName) && managerId != null) {
                User manager = userService.findById(managerId).orElseThrow();
                user.setManager(manager);
            } else {
                user.setManager(null);
            }

            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("message", "User updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id).orElseThrow();
            if (user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getName()))) {
                redirectAttributes.addFlashAttribute("error", "Cannot delete admin users");
            } else {
                userService.deleteUser(id);
                redirectAttributes.addFlashAttribute("message", "User deleted successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/rules")
    public String showRules(Model model) {
        model.addAttribute("rules", ruleService.getRulesForCompany(getCurrentCompanyId()));
        model.addAttribute("managers", userService.getUsersByRole("MANAGER"));
        model.addAttribute("company", companyService.findById(getCurrentCompanyId()).orElseThrow());
        return "admin/rules";
    }

    @PostMapping("/rules")
    public String createRule(@RequestParam String name,
                           @RequestParam String ruleType,
                           @RequestParam(required = false) Integer requiredPercentage,
                           @RequestParam(required = false) Long specificApproverId,
                           @RequestParam(required = false) String hybridType,
                           @RequestParam(required = false) BigDecimal minAmount,
                           @RequestParam(required = false) BigDecimal maxAmount,
                           @RequestParam List<Long> approvers,
                           RedirectAttributes redirectAttributes) {
        try {
            ApprovalRule rule = new ApprovalRule();
            rule.setName(name);
            rule.setCompany(companyService.findById(getCurrentCompanyId()).orElseThrow());

            // Set rule type specific configurations
            switch (ruleType) {
                case "PERCENTAGE":
                    rule.setRequiredPercentage(requiredPercentage);
                    break;
                case "SPECIFIC":
                    User specificApprover = userService.findById(specificApproverId).orElseThrow();
                    rule.setSpecificApprover(specificApprover);
                    break;
                case "HYBRID":
                    rule.setHybridRule(true);
                    rule.setRequiredPercentage(requiredPercentage);
                    User hybridApprover = userService.findById(specificApproverId).orElseThrow();
                    rule.setSpecificApprover(hybridApprover);
                    rule.setPercentageOrCfo("OR".equals(hybridType));
                    break;
            }

            // Set amount thresholds if provided
            rule.setMinAmount(minAmount);
            rule.setMaxAmount(maxAmount);

            // Create and save the rule
            ApprovalRule savedRule = ruleService.createRule(rule);

            // Set up approver sequence
            ruleService.setRuleSequence(savedRule.getId(), approvers);

            redirectAttributes.addFlashAttribute("message", "Approval rule created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating rule: " + e.getMessage());
        }
        return "redirect:/admin/rules";
    }

    @PostMapping("/rules/{id}")
    public String updateRule(@PathVariable Long id,
                           @RequestParam List<Long> approvers,
                           @RequestParam(required = false) Integer requiredPercentage,
                           @RequestParam(required = false) Long specificApproverId,
                           RedirectAttributes redirectAttributes) {
        try {
            ruleService.updateRule(id, requiredPercentage != null ? requiredPercentage : 0, specificApproverId);
            ruleService.setRuleSequence(id, approvers);
            redirectAttributes.addFlashAttribute("message", "Rule updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating rule: " + e.getMessage());
        }
        return "redirect:/admin/rules";
    }

    @DeleteMapping("/rules/{id}")
    public String deleteRule(@PathVariable Long id,
                           RedirectAttributes redirectAttributes) {
        try {
            ruleService.deleteRule(id);
            redirectAttributes.addFlashAttribute("message", "Rule deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting rule: " + e.getMessage());
        }
        return "redirect:/admin/rules";
    }

    @GetMapping("/reports")
    public String showReports(Model model) {
        // Add analytics data for admin reports
        model.addAttribute("totalUsers", userService.findAll().size());
        model.addAttribute("totalExpenses", expenseService.findAll().size());
        model.addAttribute("pendingApprovals", expenseService.findAll().stream()
                .filter(expense -> false)
                .count());
        model.addAttribute("totalCompanies", companyService.findAll().size());
        return "admin/reports";
    }

    private Long getCurrentCompanyId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                User currentUser = userService.findByUsername(username);
                if (currentUser != null && currentUser.getCompany() != null) {
                    return currentUser.getCompany().getId();
                }
            }
        }
        // Fallback to first company if no user context
        return companyService.findAll().stream()
            .findFirst()
            .map(Company::getId)
            .orElse(1L);
    }
}
