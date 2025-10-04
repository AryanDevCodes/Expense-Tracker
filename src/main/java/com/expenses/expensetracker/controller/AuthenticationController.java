package com.expenses.expensetracker.controller;

import com.expenses.expensetracker.service.AuthenticationService;
import com.expenses.expensetracker.service.CurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthenticationController {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private CurrencyService currencyService;

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        // Add available countries and their currencies for selection
        model.addAttribute("countries", currencyService.getAvailableCurrencies());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String username,
                        @RequestParam String email,
                        @RequestParam String password,
                        @RequestParam String country,
                        RedirectAttributes redirectAttributes) {
        try {
            // Check if this is first signup (will create admin + company)
            authService.signupFirstAdmin(username, email, password, country);
            redirectAttributes.addFlashAttribute("message",
                "Account created successfully! Company has been set up with " + country + " currency.");
            return "redirect:/login";
        } catch (IllegalStateException e) {
            // Not first signup, do regular user signup
            try {
                authService.signupUser(username, email, password, null);
                redirectAttributes.addFlashAttribute("message", "Account created successfully!");
                return "redirect:/login";
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
                return "redirect:/signup";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/signup";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    // Remove the conflicting createUser method - user management is handled by UserController
    // The @PostMapping("/admin/users") was conflicting with UserController's createUser method
}
