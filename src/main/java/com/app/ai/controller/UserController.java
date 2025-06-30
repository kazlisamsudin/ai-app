package com.app.ai.controller;

import com.app.ai.model.User;
import com.app.ai.model.Role;
import com.app.ai.service.UserService;
import com.app.ai.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RoleRepository roleRepository;

    @GetMapping("/list")
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> userPage = userService.findPaginatedUsers(pageable);
        model.addAttribute("userPage", userPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        return "user/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleRepository.findAll());
        return "user/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.findUserById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found");
            return "redirect:/users/list";
        }
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "user/form";
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute User user, @RequestParam(required = false) List<Long> roles, RedirectAttributes redirectAttributes) {
        Set<Role> userRoles = Set.copyOf(roleRepository.findAllById(roles));
        user.setRoles(userRoles);
        if (user.getId() == null) {
            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "User added successfully!");
        } else {
            // Editing: if password is blank, keep the old password
            User existing = userService.findUserById(user.getId()).orElse(null);
            if (existing != null && (user.getPassword() == null || user.getPassword().isBlank())) {
                user.setPassword(existing.getPassword());
            }
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
        }
        return "redirect:/users/list";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
        return "redirect:/users/list";
    }

    @GetMapping("/view/{id}")
    public String viewUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return userService.findUserById(id)
            .map(user -> {
                model.addAttribute("user", user);
                return "user/view";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                return "redirect:/users/list";
            });
    }
}
