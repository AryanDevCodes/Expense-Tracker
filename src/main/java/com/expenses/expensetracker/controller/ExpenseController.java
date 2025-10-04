package com.expenses.expensetracker.controller;

import com.expenses.expensetracker.entity.Expense;
import com.expenses.expensetracker.entity.ExpenseStatus;
import com.expenses.expensetracker.entity.User;
import com.expenses.expensetracker.dto.ExpenseOcrResult;
import com.expenses.expensetracker.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ApprovalWorkflowService workflowService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listExpenses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("currencies", currencyService.getAvailableCurrencies());

        if (userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            // Admin should see ALL expenses in the system
            model.addAttribute("expenses", expenseService.findAll());
            return "expenses/list";
        } else if (userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"))) {
            // Manager sees team expenses
            model.addAttribute("expenses", workflowService.getTeamExpenses(getUserId(userDetails)));
            return "expenses/list";
        } else {
            // Employee sees only their own expenses
            model.addAttribute("expenses", expenseService.getEmployeeExpenses(getUserId(userDetails)));
            return "expenses/list";
        }
    }

    @GetMapping("/submit")
    public String showSubmitForm(Model model) {
        model.addAttribute("expense", new Expense());
        model.addAttribute("currencies", currencyService.getAvailableCurrencies());
        return "expenses/submit";
    }

    @PostMapping("/submit")
    public String submitExpense(@ModelAttribute Expense expense,
                              @RequestParam(required = false) MultipartFile receipt,
                              @RequestParam(required = false) Boolean useOcr,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            if (useOcr != null && useOcr && receipt != null && !receipt.isEmpty()) {
                ExpenseOcrResult ocrResult = ocrService.processReceipt(receipt.getBytes());
                if (ocrResult.isSuccess()) {
                    expense.setAmount(ocrResult.getAmount());
                    expense.setCurrency(ocrResult.getCurrency());
                    expense.setDate(ocrResult.getDate());
                    expense.setDescription(ocrResult.getDescription());
                    expense.setCategory(ocrResult.getExpenseType());
                    expense.setReceiptUrl(saveReceipt(receipt));
                }
            }

            User submitter = userService.findByUsername(userDetails.getUsername());
            expense.setSubmitter(submitter);
            expense.setCompany(submitter.getCompany());

            if (!expense.getCurrency().equals(submitter.getCompanyCurrency())) {
                BigDecimal convertedAmount = currencyService.convertAmount(
                    expense.getAmount(),
                    expense.getCurrency(),
                    submitter.getCompanyCurrency()
                );
                expense.setBaseCurrencyAmount(convertedAmount);
                expense.setExchangeRate(currencyService.getExchangeRate(
                    expense.getCurrency(),
                    submitter.getCompanyCurrency()
                ));
            }

            Expense savedExpense = expenseService.submitExpense(expense);

            // Use appropriate workflow based on user role
            if (submitter.hasRole("MANAGER")) {
                workflowService.initiateManagerExpenseWorkflow(savedExpense.getId());
            } else {
                workflowService.initiateWorkflow(savedExpense.getId());
            }

            redirectAttributes.addFlashAttribute("message", "Expense submitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error submitting expense: " + e.getMessage());
        }
        return "redirect:/expenses";
    }

    @GetMapping("/pending")
    public String listPendingApprovals(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Long userId = getUserId(userDetails);
        List<Expense> pendingExpenses;

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isManager = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        if (isAdmin) {
            // Admin sees ALL pending approvals across the system for override capability
            pendingExpenses = workflowService.getAllPendingApprovals();
            model.addAttribute("canOverrideApprovals", true);
        } else if (isManager) {
            // Manager sees only their team's pending approvals
            pendingExpenses = workflowService.getPendingApprovalsForUser(userId);
            model.addAttribute("canOverrideApprovals", false);
        } else {
            // Employees cannot access pending approvals
            return "redirect:/dashboard";
        }

        // Get current user's company default currency for proper display
        User currentUser = userService.findByUsername(userDetails.getUsername());
        String companyCurrency = currentUser.getCompany() != null ?
            currentUser.getCompany().getDefaultCurrency() : "INR";

        model.addAttribute("pendingExpenses", pendingExpenses);
        model.addAttribute("companyCurrency", companyCurrency);

        // Convert amounts to company default currency for managers/admins
        model.addAttribute("convertedAmounts",
            pendingExpenses.stream()
                .collect(java.util.stream.Collectors.toMap(
                    Expense::getId,
                    e -> expenseService.convertToCompanyCurrency(e.getId())
                )));

        return "expenses/pending";
    }

    @PostMapping("/{id}/approve")
    public String approveExpense(@PathVariable Long id,
                               @RequestParam(required = false, defaultValue = "") String comments,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            // Check if user has permission to approve expenses
            if (!canUserApproveExpenses(userDetails, id)) {
                redirectAttributes.addFlashAttribute("error", "Access denied. You do not have permission to approve this expense.");
                return "redirect:/expenses/pending";
            }

            Long approverId = getUserId(userDetails);
            if (userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                // Admin can approve any expense directly
                workflowService.processDirectorApproval(id, approverId, comments);
            } else if (userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"))) {
                workflowService.processManagerApproval(id, approverId, comments);
            }

            redirectAttributes.addFlashAttribute("message", "Expense approved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error approving expense: " + e.getMessage());
        }
        return "redirect:/expenses/pending";
    }

    @PostMapping("/{id}/reject")
    public String rejectExpense(@PathVariable Long id,
                              @RequestParam String reason,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            // Check if user has permission to reject expenses
            if (!canUserApproveExpenses(userDetails, id)) {
                redirectAttributes.addFlashAttribute("error", "Access denied. You do not have permission to reject this expense.");
                return "redirect:/expenses/pending";
            }

            workflowService.rejectExpense(id, getUserId(userDetails), reason);
            redirectAttributes.addFlashAttribute("message", "Expense rejected successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error rejecting expense: " + e.getMessage());
        }
        return "redirect:/expenses/pending";
    }

    @GetMapping("/{id}")
    public String viewExpense(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        Optional<Expense> expenseOpt = expenseService.findById(id);
        if (expenseOpt.isEmpty()) {
            model.addAttribute("error", "Expense not found");
            return "redirect:/expenses";
        }

        Expense expense = expenseOpt.get();
        User currentUser = userService.findByUsername(userDetails.getUsername());

        // Check if user has permission to view this expense
        boolean canView = currentUser.hasRole("ADMIN") ||
                         expense.getSubmitter().getId().equals(currentUser.getId()) ||
                         (currentUser.hasRole("MANAGER") && isManagerOf(currentUser, expense.getSubmitter()));

        if (!canView) {
            model.addAttribute("error", "Access denied");
            return "redirect:/expenses";
        }

        model.addAttribute("expense", expense);
        model.addAttribute("approvalSteps", expense.getApprovalSteps());
        model.addAttribute("canApprove", canUserApprove(currentUser, expense));
        return "expenses/view";
    }

    @GetMapping("/{id}/approve")
    public String showApproveForm(@PathVariable Long id, Model model) {
        expenseService.findById(id).ifPresent(expense -> {
            model.addAttribute("expense", expense);
        });
        return "expenses/approve";
    }

    @GetMapping("/{id}/reject")
    public String showRejectForm(@PathVariable Long id, Model model) {
        expenseService.findById(id).ifPresent(expense -> {
            model.addAttribute("expense", expense);
        });
        return "expenses/reject";
    }

    @PostMapping("/{id}/override-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public String overrideApproval(@PathVariable Long id,
                                  @RequestParam(required = false, defaultValue = "") String comments,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            Long adminId = getUserId(userDetails);

            // Admin override - directly approve regardless of current workflow state
            Expense expense = expenseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

            // Create override approval step
            workflowService.processAdminOverride(id, adminId, "ADMIN OVERRIDE: " + comments);

            redirectAttributes.addFlashAttribute("message",
                "Expense approved via admin override!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Error overriding approval: " + e.getMessage());
        }
        return "redirect:/expenses/pending";
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasRole('MANAGER')")
    public String escalateExpense(@PathVariable Long id,
                                 @RequestParam String escalationReason,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long managerId = getUserId(userDetails);

            // Manager escalation based on approval rules
            workflowService.escalateExpense(id, managerId, escalationReason);

            redirectAttributes.addFlashAttribute("message",
                "Expense escalated to higher authority!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Error escalating expense: " + e.getMessage());
        }
        return "redirect:/expenses/pending";
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

    private String saveReceipt(MultipartFile receipt) throws IOException {
        String fileName = String.format("%s-%s%s",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
            UUID.randomUUID().toString().substring(0, 8),
            getFileExtension(receipt.getOriginalFilename())
        );

        Path uploadPath = Paths.get("uploads/receipts");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(receipt.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/receipts/" + fileName;
    }

    private String getFileExtension(String filename) {
        return Optional.ofNullable(filename)
            .filter(f -> f.contains("."))
            .map(f -> f.substring(f.lastIndexOf(".")))
            .orElse(".jpg");
    }

    private boolean isManagerOf(User manager, User employee) {
        return employee.getManager() != null && employee.getManager().getId().equals(manager.getId());
    }

    private boolean canUserApprove(User user, Expense expense) {
        return user.hasRole("ADMIN") ||
               (user.hasRole("MANAGER") && isManagerOf(user, expense.getSubmitter()));
    }

    private boolean canUserApproveExpenses(UserDetails userDetails, Long expenseId) {
        // Check if user is admin
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return true; // Admin can approve any expense regardless of status
        }

        // Check if user is manager
        boolean isManager = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        if (!isManager) {
            return false; // Only admins and managers can approve expenses
        }

        // For managers, check if the expense is pending their approval
        Optional<Expense> expenseOpt = expenseService.findById(expenseId);
        if (expenseOpt.isEmpty()) {
            return false;
        }

        Expense expense = expenseOpt.get();
        ExpenseStatus status = expense.getStatus();

        // Manager can only approve expenses that are pending manager approval
        if (status == ExpenseStatus.PENDING_MANAGER) {
            // Check if this manager is the designated approver for this expense
            User currentUser = userService.findByUsername(userDetails.getUsername());
            return isManagerOf(currentUser, expense.getSubmitter());
        }

        return false; // Manager cannot approve expenses in other states
    }
}
