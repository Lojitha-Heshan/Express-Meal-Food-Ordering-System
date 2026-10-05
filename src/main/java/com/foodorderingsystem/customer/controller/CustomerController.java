package com.foodorderingsystem.customer.controller;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.customer.service.CustomerService;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customer/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("customer") Customer customer, BindingResult result) {
        if (result.hasErrors()) {
            return "customer/register";
        }
        customerService.register(customer);
        return "redirect:/login";
    }

    // Login/logout are handled by the unified AuthController (/login, /logout)

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) {
            return "redirect:/login";
        }
        List<Order> orders = orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
        model.addAttribute("customer", customer);
        model.addAttribute("orders", orders);
        model.addAttribute("orderCount", orders.size());
        return "customer/dashboard";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Customer sessionCustomer = (Customer) session.getAttribute("loggedInCustomer");
        if (sessionCustomer == null) {
            return "redirect:/login";
        }
        // Always re-fetch fresh data from the DB so the form shows the latest saved values
        Customer customer = customerService.getById(sessionCustomer.getCustomerId());
        model.addAttribute("customer", customer);
        return "customer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(HttpSession session,
                                @RequestParam String name,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Customer sessionCustomer = (Customer) session.getAttribute("loggedInCustomer");
        if (sessionCustomer == null) {
            return "redirect:/login";
        }

        if (name == null || name.isBlank()) {
            model.addAttribute("customer", customerService.getById(sessionCustomer.getCustomerId()));
            model.addAttribute("error", "Name can't be empty.");
            return "customer/profile";
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 4) {
                model.addAttribute("customer", customerService.getById(sessionCustomer.getCustomerId()));
                model.addAttribute("error", "New password should be at least 4 characters.");
                return "customer/profile";
            }
            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("customer", customerService.getById(sessionCustomer.getCustomerId()));
                model.addAttribute("error", "New password and confirmation don't match.");
                return "customer/profile";
            }
        }

        try {
            Customer updated = customerService.updateProfile(
                    sessionCustomer.getCustomerId(), name, phone, address, currentPassword, newPassword);
            // keep the session in sync so navbar/dashboard show the fresh info immediately
            session.setAttribute("loggedInCustomer", updated);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            return "redirect:/customer/profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("customer", customerService.getById(sessionCustomer.getCustomerId()));
            model.addAttribute("error", ex.getMessage());
            return "customer/profile";
        }
    }
}
