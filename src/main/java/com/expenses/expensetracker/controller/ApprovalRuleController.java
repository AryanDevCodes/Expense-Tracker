package com.expenses.expensetracker.controller;

import com.expenses.expensetracker.entity.ApprovalRule;
import com.expenses.expensetracker.service.ApprovalRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/approval-rules")
@PreAuthorize("hasRole('ADMIN')")
public class ApprovalRuleController {

    @Autowired
    private ApprovalRuleService approvalRuleService;

    @GetMapping
    public String listRules(Model model) {
        List<ApprovalRule> rules = approvalRuleService.findAll();
        model.addAttribute("rules", rules);
        return "admin/rules";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("rule", new ApprovalRule());
        return "admin/rule-form";
    }

    @PostMapping
    public String createRule(@ModelAttribute ApprovalRule rule,
                            RedirectAttributes redirectAttributes) {
        try {
            approvalRuleService.createRule(rule);
            redirectAttributes.addFlashAttribute("message", "Approval rule created successfully!");
            return "redirect:/approval-rules";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating rule: " + e.getMessage());
            return "redirect:/approval-rules/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        ApprovalRule rule = approvalRuleService.findById(id).orElse(null);
        if (rule == null) {
            return "redirect:/approval-rules";
        }
        model.addAttribute("rule", rule);
        return "admin/rule-form";
    }

    @PostMapping("/{id}")
    public String updateRule(@PathVariable Long id,
                            @ModelAttribute ApprovalRule rule,
                            RedirectAttributes redirectAttributes) {
        try {
            ApprovalRule existingRule = approvalRuleService.findById(id).orElse(null);
            if (existingRule == null) {
                redirectAttributes.addFlashAttribute("error", "Rule not found");
                return "redirect:/approval-rules";
            }

            existingRule.setName(rule.getName());
            existingRule.setMinAmount(rule.getMinAmount());
            existingRule.setMaxAmount(rule.getMaxAmount());
            existingRule.setRequiredPercentage(rule.getRequiredPercentage());
            existingRule.setRequiresManagerFirst(rule.isRequiresManagerFirst());

            approvalRuleService.updateApprovalRule(existingRule);
            redirectAttributes.addFlashAttribute("message", "Rule updated successfully!");
            return "redirect:/approval-rules";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating rule: " + e.getMessage());
            return "redirect:/approval-rules/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteRule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            approvalRuleService.deleteRule(id);
            redirectAttributes.addFlashAttribute("message", "Rule deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting rule: " + e.getMessage());
        }
        return "redirect:/approval-rules";
    }
}
