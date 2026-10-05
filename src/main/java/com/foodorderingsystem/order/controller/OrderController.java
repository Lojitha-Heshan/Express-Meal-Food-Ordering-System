package com.foodorderingsystem.order.controller;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.order.builder.OrderReceipt;
import com.foodorderingsystem.order.builder.OrderReceiptBuilder;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.service.PaymentService;
import com.foodorderingsystem.restaurant.entity.MenuItem;
import com.foodorderingsystem.restaurant.service.MenuItemService;
import com.foodorderingsystem.delivery.service.DeliveryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DeliveryService deliveryService;

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new LinkedHashMap<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    @PostMapping("/add/{itemId}")
    public String addToCart(@PathVariable Long itemId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            HttpSession session) {
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }
        if (quantity > 20) {
            quantity = 20;
        }
        Map<Long, Integer> cart = getCart(session);
        cart.merge(itemId, quantity, Integer::sum);
        // cap the running total per item too, not just each individual add
        if (cart.get(itemId) > 20) {
            cart.put(itemId, 20);
        }
        return "redirect:/restaurant/menu";
    }

    @GetMapping("/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId, HttpSession session) {
        Map<Long, Integer> cart = getCart(session);
        cart.remove(itemId);
        return "redirect:/order/cart";
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        Map<Long, Integer> cart = getCart(session);
        Map<MenuItem, Integer> cartItems = new LinkedHashMap<>();
        double total = 0;
        boolean someItemsRemoved = false;
        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            MenuItem item;
            try {
                item = menuItemService.getById(entry.getKey());
            } catch (RuntimeException ex) {
                // item was deleted from the menu after being added to this cart - drop it silently
                // (handled below the loop) instead of crashing the whole cart page
                someItemsRemoved = true;
                continue;
            }
            cartItems.put(item, entry.getValue());
            total += item.getPrice() * entry.getValue();
        }
        if (someItemsRemoved) {
            // clean the stale ids out of the session cart so this doesn't repeat on every visit
            cart.keySet().removeIf(id -> cartItems.keySet().stream().noneMatch(i -> i.getItemId().equals(id)));
            model.addAttribute("notice", "One or more items in your cart are no longer available and were removed.");
        }
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("total", total);
        model.addAttribute("customer", session.getAttribute("loggedInCustomer"));
        return "order/cart";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam(required = false) String contactPhone,
                           @RequestParam(required = false) String houseNo,
                           @RequestParam(required = false) String street,
                           @RequestParam(required = false) String city,
                           @RequestParam(required = false) String postalCode,
                           @RequestParam(required = false) String landmark,
                           HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) {
            return "redirect:/customer/login";
        }
        Map<Long, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/order/cart";
        }

        // real server-side validation - the HTML5 "required"/pattern attributes alone can be bypassed
        String addressError = null;
        if (contactPhone == null || !contactPhone.trim().matches("^[0-9]{9,15}$")) {
            addressError = "Enter a valid contact number (9-15 digits).";
        } else if (houseNo == null || houseNo.isBlank()) {
            addressError = "House / Unit No. is required.";
        } else if (street == null || street.isBlank()) {
            addressError = "Street address is required.";
        } else if (city == null || city.isBlank()) {
            addressError = "City is required.";
        } else if (postalCode != null && !postalCode.isBlank() && !postalCode.trim().matches("^[0-9]{4,6}$")) {
            addressError = "Postal code must be 4-6 digits.";
        }

        if (addressError != null) {
            Map<MenuItem, Integer> cartItems = new LinkedHashMap<>();
            double total = 0;
            for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
                MenuItem item = menuItemService.getById(entry.getKey());
                cartItems.put(item, entry.getValue());
                total += item.getPrice() * entry.getValue();
            }
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("total", total);
            model.addAttribute("customer", customer);
            model.addAttribute("error", addressError);
            // echo back what they typed so they don't have to retype everything
            model.addAttribute("contactPhone", contactPhone);
            model.addAttribute("houseNo", houseNo);
            model.addAttribute("street", street);
            model.addAttribute("city", city);
            model.addAttribute("postalCode", postalCode);
            model.addAttribute("landmark", landmark);
            return "order/cart";
        }

        StringBuilder address = new StringBuilder();
        address.append(houseNo.trim()).append(", ").append(street.trim()).append(", ").append(city.trim());
        if (postalCode != null && !postalCode.isBlank()) {
            address.append(" ").append(postalCode.trim());
        }
        if (landmark != null && !landmark.isBlank()) {
            address.append(" (").append(landmark.trim()).append(")");
        }
        address.append(" | Contact: ").append(contactPhone.trim());

        Order order = orderService.placeOrder(customer, cart, address.toString());
        session.setAttribute("cart", new HashMap<Long, Integer>()); // clear cart
        return "redirect:/payment/checkout/" + order.getOrderId();
    }

    @GetMapping("/history")
    public String orderHistory(HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) {
            return "redirect:/customer/login";
        }
        model.addAttribute("orders", orderService.getOrdersForCustomer(customer));
        return "order/history";
    }

    // Customer cancels their own order - only allowed while it's still PENDING/CONFIRMED
    @PostMapping("/cancel/{id}")
    public String cancelOrder(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) {
            return "redirect:/customer/login";
        }
        try {
            orderService.cancelOrder(id, customer.getCustomerId());
            redirectAttributes.addFlashAttribute("success", "Order #" + id + " has been cancelled.");
        } catch (SecurityException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/order/history";
    }

    @GetMapping("/track/{id}")
    public String trackOrder(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getById(id));
        model.addAttribute("delivery", deliveryService.getByOrderId(id));
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "order/track";
    }

    // Builder pattern: OrderReceiptBuilder assembles the receipt step by step
    // (order details -> line items -> payment info -> grand total) before handing
    // back the finished OrderReceipt for this printable view.
    @GetMapping("/receipt/{id}")
    public String viewReceipt(@PathVariable Long id, Model model) {
        Order order = orderService.getById(id);
        Payment payment = null;
        try {
            payment = paymentService.getByOrderId(id);
        } catch (RuntimeException ignored) {
            // order hasn't been paid for yet - receipt just shows no payment section
        }

        OrderReceipt receipt = new OrderReceiptBuilder().start()
                .withOrderDetails(order)
                .withLineItems(order)
                .withPayment(payment)
                .calculateGrandTotal()
                .build();

        model.addAttribute("receipt", receipt);
        return "order/receipt";
    }
}
