package com.stylehub.controller;

import com.stylehub.dto.PasswordChangeDto;
import com.stylehub.dto.RegisterDto;
import com.stylehub.model.User;
import com.stylehub.service.OrderService;
import com.stylehub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    private final UserService userService;
    private final OrderService orderService;

    public UserController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerDto") RegisterDto registerDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(registerDto);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully! Please sign in.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        User user = userService.getByEmail(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("addresses", userService.getUserAddresses(user.getId()));
        model.addAttribute("recentOrders", orderService.getCustomerOrders(user.getEmail()).stream().limit(5).toList());
        model.addAttribute("passwordDto", new PasswordChangeDto());
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            userService.updateProfile(auth.getName(), name, phone, address);
            redirectAttributes.addFlashAttribute("profileSuccess", "Profile updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("profileError", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(
            @ModelAttribute PasswordChangeDto passwordDto,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            userService.changePassword(auth.getName(), passwordDto);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Password changed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/address/add")
    public String addAddress(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam String streetAddress,
            @RequestParam String city,
            @RequestParam String state,
            @RequestParam String postalCode,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            userService.addAddress(auth.getName(), fullName, phone, streetAddress, city, state, postalCode);
            redirectAttributes.addFlashAttribute("addressSuccess", "New delivery address saved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("addressError", e.getMessage());
        }
        return "redirect:/profile";
    }
}
