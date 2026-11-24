package com.stylehub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    @GetMapping("/contact")
    public String contact(Model model) {
        return "contact";
    }

    @PostMapping("/contact/submit")
    public String submitContact(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String message,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("successMessage",
                "Thank you, " + name + "! Your inquiry has been received. Our style concierge team will reply within 24 hours.");
        return "redirect:/contact";
    }
}
