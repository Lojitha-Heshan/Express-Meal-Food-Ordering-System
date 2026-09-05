package com.foodorderingsystem.payment.controller;

import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/checkout/{orderId}")
    public String showCheckout(@PathVariable Long orderId, Model model) {
        model.addAttribute("order", orderService.getById(orderId));
        return "payment/checkout";
    }

    @PostMapping("/pay")
    public String pay(@RequestParam Long orderId, @RequestParam String method, Model model) {
        Payment payment = paymentService.processPayment(orderId, method);
        return "redirect:/payment/invoice/" + payment.getOrder().getOrderId();
    }

    @GetMapping("/invoice/{orderId}")
    public String showInvoice(@PathVariable Long orderId, Model model) {
        model.addAttribute("order", orderService.getById(orderId));
        model.addAttribute("payment", paymentService.getByOrderId(orderId));
        return "payment/invoice";
    }

    // Simple admin sales report
    @GetMapping("/reports")
    public String salesReport(Model model) {
        model.addAttribute("payments", paymentService.getAllPayments());
        model.addAttribute("totalRevenue", paymentService.getTotalRevenue());
        return "payment/reports";
    }
}
