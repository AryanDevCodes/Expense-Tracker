package com.expenses.expensetracker.controller;

import com.expenses.expensetracker.entity.User;
import com.expenses.expensetracker.entity.Role;
import com.expenses.expensetracker.service.UserService;
import com.expenses.expensetracker.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    // Constructor injection instead of field injection
    public UserController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "users/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("user", new User());

        // Add role-based permissions for user creation
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isManager = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isManager", isManager);

        // Add available roles based on current user's permissions
        addRolesAndManagersToModel(model, isAdmin, isManager);

        return "users/form";
    }

    @PostMapping
    public String createUser(@ModelAttribute User user,
                           @RequestParam String roleName,
                           @RequestParam(required = false) Long managerId,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        try {
            // Check permissions based on current user role
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isManager = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

            // Validate role creation permissions
            if (isManager && roleName.equals("MANAGER")) {
                redirectAttributes.addFlashAttribute("error", "Managers cannot create other managers");
                return "redirect:/users/new";
            }

            if (!isAdmin && !isManager) {
                redirectAttributes.addFlashAttribute("error", "Access denied");
                return "redirect:/dashboard";
            }

            // Admin can create Employee or Manager roles, Manager can only create Employee
            if (isAdmin && (!roleName.equals("EMPLOYEE") && !roleName.equals("MANAGER"))) {
                redirectAttributes.addFlashAttribute("error", "Invalid role selection");
                return "redirect:/users/new";
            } else if (isManager && !roleName.equals("EMPLOYEE")) {
                redirectAttributes.addFlashAttribute("error", "Managers can only create employee accounts");
                return "redirect:/users/new";
            }

            // Assign role
            Role role = roleService.findByName(roleName).orElse(null);
            if (role == null) {
                redirectAttributes.addFlashAttribute("error", "Role not found: " + roleName);
                return "redirect:/users/new";
            }
            user.getRoles().add(role);

            // Assign manager if creating an employee
            if (roleName.equals("EMPLOYEE")) {
                if (managerId != null) {
                    userService.findById(managerId).ifPresent(user::setManager);
                } else if (isManager) {
                    // If manager is creating employee without specifying manager, set current user as manager
                    User currentManager = userService.findByUsername(userDetails.getUsername());
                    user.setManager(currentManager);
                }
            }

            // Set company (same as creator's company)
            User creator = userService.findByUsername(userDetails.getUsername());
            if (creator != null && creator.getCompany() != null) {
                user.setCompany(creator.getCompany());
            }

            userService.createUser(user);
            redirectAttributes.addFlashAttribute("message",
                roleName + " created successfully!");

            // Redirect based on user role
            if (isAdmin) {
                return "redirect:/users";
            } else {
                return "redirect:/dashboard";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Error creating user: " + e.getMessage());
            return "redirect:/users/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        // Fix MVC view resolution issue - use traditional Optional handling
        User user = userService.findById(id).orElse(null);
        if (user == null) {
            return "redirect:/admin/users";
        }

        model.addAttribute("user", user);

        // Use extracted method to avoid code duplication
        addRolesAndManagersToModel(model);

        // Add current user's role for form pre-selection
        String currentRole = user.getRoles().isEmpty() ? "EMPLOYEE" :
                           user.getRoles().iterator().next().getName();
        model.addAttribute("currentRole", currentRole);

        return "users/form";
    }

    @PostMapping("/{id}")
    public String updateUser(@PathVariable Long id,
                           @ModelAttribute User user,
                           @RequestParam String roleName,
                           @RequestParam(required = false) Long managerId,
                           RedirectAttributes redirectAttributes) {
        try {
            // Fix MVC view resolution issue - use traditional Optional handling
            User existingUser = userService.findById(id).orElse(null);
            if (existingUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/users";
            }

            // Admin can change roles between Employee and Manager
            if (!roleName.equals("EMPLOYEE") && !roleName.equals("MANAGER")) {
                redirectAttributes.addFlashAttribute("error", "Invalid role selection");
                return "redirect:/admin/users/" + id + "/edit";
            }

            // Update role - fix Optional<Role> issue
            existingUser.getRoles().clear();
            Role role = roleService.findByName(roleName).orElse(null);
            if (role == null) {
                redirectAttributes.addFlashAttribute("error", "Role not found: " + roleName);
                return ("redirect:/admin/users/" + id + "/edit");
            }
            existingUser.getRoles().add(role);

            // Update manager relationship - fix Optional<User> issue
            if (roleName.equals("EMPLOYEE") && managerId != null) {
                userService.findById(managerId).ifPresent(existingUser::setManager);
            } else {
                existingUser.setManager(null);
            }

            // Update basic info
            existingUser.setEmail(user.getEmail());
            existingUser.setUsername(user.getUsername());

            userService.updateUser(existingUser);
            redirectAttributes.addFlashAttribute("message", "User updated successfully!");
            return "redirect:/admin/users";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Error updating user: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Fix Optional<User> issue - use proper Optional handling
            userService.findById(id).ifPresentOrElse(
                user -> {
                    if (!user.hasRole("ADMIN")) {
                        userService.deleteUser(id);
                        redirectAttributes.addFlashAttribute("message", "User deleted successfully!");
                    } else {
                        redirectAttributes.addFlashAttribute("error", "Cannot delete admin user");
                    }
                },
                () -> redirectAttributes.addFlashAttribute("error", "User not found")
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Extracted method to eliminate code duplication for loading roles and managers
     */
    private void addRolesAndManagersToModel(Model model, boolean isAdmin, boolean isManager) {
        // Only show Employee and Manager roles (Admin creates these)
        Role employeeRole = roleService.findByName("EMPLOYEE").orElse(null);
        Role managerRole = roleService.findByName("MANAGER").orElse(null);

        List<Role> availableRoles = new java.util.ArrayList<>();
        if (isAdmin) {
            // Admin can create both employees and managers
            if (employeeRole != null) availableRoles.add(employeeRole);
            if (managerRole != null) availableRoles.add(managerRole);
        } else if (isManager) {
            // Manager can only create employees
            if (employeeRole != null) availableRoles.add(employeeRole);
        }
        model.addAttribute("roles", availableRoles);

        // Get potential managers for assignment
        List<User> managers = userService.getUsersByRole("MANAGER");
        model.addAttribute("managers", managers != null ? managers : List.of());
    }

    // Overloaded method for backward compatibility
    private void addRolesAndManagersToModel(Model model) {
        Role employeeRole = roleService.findByName("EMPLOYEE").orElse(null);
        Role managerRole = roleService.findByName("MANAGER").orElse(null);

        List<Role> availableRoles = List.of();
        if (employeeRole != null && managerRole != null) {
            availableRoles = List.of(employeeRole, managerRole);
        }
        model.addAttribute("roles", availableRoles);

        List<User> managers = userService.getUsersByRole("MANAGER");
        model.addAttribute("managers", managers != null ? managers : List.of());
    }
}
