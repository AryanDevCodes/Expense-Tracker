package com.expenses.expensetracker.controller;

import com.expenses.expensetracker.entity.Company;
import com.expenses.expensetracker.service.CompanyService;
import com.expenses.expensetracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/companies")
@PreAuthorize("hasRole('ADMIN')")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listCompanies(Model model) {
        List<Company> companies = companyService.findAll();
        model.addAttribute("companies", companies);
        return "companies/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("company", new Company());
        return "companies/form";
    }

    @PostMapping
    public String createCompany(@ModelAttribute Company company,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            // Set default currency if not specified
            if (company.getDefaultCurrency() == null || company.getDefaultCurrency().isEmpty()) {
                company.setDefaultCurrency("INR");
            }

            Company savedCompany = companyService.createCompany(company);

            // Auto-assign admin to this company if they don't have one
            var admin = userService.findByUsername(userDetails.getUsername());
            if (admin.getCompany() == null) {
                admin.setCompany(savedCompany);
                userService.updateUser(admin);
            }

            redirectAttributes.addFlashAttribute("message", "Company created successfully!");
            return "redirect:/companies";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating company: " + e.getMessage());
            return "redirect:/companies/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Company company = companyService.findById(id).orElse(null);
        if (company == null) {
            return "redirect:/companies";
        }
        model.addAttribute("company", company);
        return "companies/form";
    }

    @PostMapping("/{id}")
    public String updateCompany(@PathVariable Long id,
                               @ModelAttribute Company company,
                               RedirectAttributes redirectAttributes) {
        try {
            Company existingCompany = companyService.findById(id).orElse(null);
            if (existingCompany == null) {
                redirectAttributes.addFlashAttribute("error", "Company not found");
                return "redirect:/companies";
            }

            existingCompany.setName(company.getName());
            existingCompany.setDefaultCurrency(company.getDefaultCurrency());
            existingCompany.setAddress(company.getAddress());
            existingCompany.setContactEmail(company.getContactEmail());

            companyService.updateCompany(existingCompany);
            redirectAttributes.addFlashAttribute("message", "Company updated successfully!");
            return "redirect:/companies";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating company: " + e.getMessage());
            return "redirect:/companies/" + id + "/edit";
        }
    }
}
