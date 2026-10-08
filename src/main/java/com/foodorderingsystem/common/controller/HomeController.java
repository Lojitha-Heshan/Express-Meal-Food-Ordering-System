package com.foodorderingsystem.common.controller;

import com.foodorderingsystem.admin.singleton.AppSettings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String homePage(Model model) {
        // Singleton pattern: reads the same AppSettings instance the admin dashboard writes to
        model.addAttribute("appSettings", AppSettings.getInstance());
        return "index";
    }
}