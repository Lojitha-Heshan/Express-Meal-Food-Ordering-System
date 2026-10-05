package com.foodorderingsystem.payment.controller;

import com.foodorderingsystem.customer.service.CustomerService;
import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    // Sales reports / CSV export are admin-only
    private boolean notAdmin(HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        return isAdmin == null || !isAdmin;
    }

    @GetMapping("/checkout/{orderId}")
    public String showCheckout(@PathVariable Long orderId, Model model) {
        model.addAttribute("order", orderService.getById(orderId));
        model.addAttribute("deliveryFee", PaymentService.DELIVERY_FEE);
        return "payment/checkout";
    }

    @PostMapping("/pay")
    public String pay(@RequestParam Long orderId, @RequestParam String method, Model model) {
        // guard against a tampered/unexpected method value bypassing the client-side selector
        if (!"CARD".equals(method) && !"CASH_ON_DELIVERY".equals(method)) {
            model.addAttribute("order", orderService.getById(orderId));
            model.addAttribute("error", "Invalid payment method selected.");
            return "payment/checkout";
        }
        Payment payment = paymentService.processPayment(orderId, method);
        return "redirect:/payment/invoice/" + payment.getOrder().getOrderId();
    }

    @GetMapping("/invoice/{orderId}")
    public String showInvoice(@PathVariable Long orderId, Model model) {
        model.addAttribute("order", orderService.getById(orderId));
        model.addAttribute("payment", paymentService.getByOrderId(orderId));
        return "payment/invoice";
    }

    // Full admin report: sales (with optional date filter), customer activity, top-selling items
    @GetMapping("/reports")
    public String salesReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                              HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        List<Payment> payments = paymentService.getPaymentsBetween(startDate, endDate);
        double totalRevenue = payments.stream().mapToDouble(Payment::getAmount).sum();

        model.addAttribute("payments", payments);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("revenueBreakdown", paymentService.computeRevenueBreakdown(payments));
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        // Customer activity
        model.addAttribute("totalCustomers", customerService.getTotalCustomerCount());
        model.addAttribute("totalOrders", orderService.getAllOrders().size());

        // Top-selling items
        Map<String, Integer> topItems = orderService.getTopSellingItems();
        model.addAttribute("topItems", topItems);

        return "payment/reports";
    }

    // Export the currently filtered report as CSV
    @GetMapping("/reports/export")
    public void exportCsv(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                          HttpSession session,
                          HttpServletResponse response) throws Exception {
        if (notAdmin(session)) {
            response.sendRedirect("/login");
            return;
        }
        List<Payment> payments = paymentService.getPaymentsBetween(startDate, endDate);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"sales_report.csv\"");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        PrintWriter writer = response.getWriter();
        writer.println("Order ID,Amount (Rs.),Method,Status,Date");
        for (Payment p : payments) {
            writer.println(
                    p.getOrder().getOrderId() + "," +
                            p.getAmount() + "," +
                            p.getMethod() + "," +
                            p.getStatus() + "," +
                            p.getPaidAt().format(fmt)
            );
        }
        writer.flush();
    }
}
