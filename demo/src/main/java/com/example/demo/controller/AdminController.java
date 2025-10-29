package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/Admin")
public class AdminController {

    // Mapping hiện có: trả về toàn bộ layout
    @GetMapping("/home")
    public String home(Model model) {
        return "Admin/home";
    }

    @GetMapping("/addproduct")
    public String addProduct(Model model) {
       
        return "Admin/addproduct";
    }
    @GetMapping("/fixproduct")
    public String fixProduct(Model model) {
        
        return "Admin/fixproduct";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
       
        return "Admin/order";
    }
    @GetMapping("/report")
    public String reports(Model model) {
       
        return "Admin/report";
    }

    @GetMapping("/tonkho")
    public String tonkho(Model model) {
       
        return "Admin/tonkho";
    }
    @GetMapping("/export")
    public String export(Model model) {
       
        return "Admin/export";
    }
    @GetMapping("/inventory")
    public String inventory(Model model) {
       
        return "Admin/inventory";
    }
    
}