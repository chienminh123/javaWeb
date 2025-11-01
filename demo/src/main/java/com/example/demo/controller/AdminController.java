package com.example.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Genre;
import com.example.demo.model.Provider;
import com.example.demo.service.GenreService;
import com.example.demo.service.ProductService;
import com.example.demo.service.ProviderService;

@Controller
@RequestMapping("/Admin")
public class AdminController {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private GenreService genreService;

    @Autowired
    private ProductService productService;

    // === CÁC MAPPING ===
    @GetMapping("/home")
    public String home(Model model) {
        return "Admin/home";
    }

    @GetMapping("/addproduct")
    public String addProduct(Model model) {
        model.addAttribute("providers", providerService.findAll());
        model.addAttribute("genres", genreService.findAllGenres()); // List<Genre>
        model.addAttribute("productSuggestions", productService.getAllProductSuggestionsMap());
        return "Admin/addproduct";
    }

    @PostMapping("/saveMultipleProducts")
    public String saveMultipleProducts(
        @RequestParam String[] productName,
        @RequestParam Integer[] providerId,
        @RequestParam Integer[] genreId,
        @RequestParam Float[] basisPrice,
        @RequestParam String[] description,
        @RequestParam(required = false) MultipartFile[][] images,
       
        @RequestParam(required = false) String[][] sizeName,
        @RequestParam(required = false) Integer[][] quantity
        
    ) {
        productService.saveMultipleProducts(
            productName, providerId, genreId, basisPrice, description, images,
            sizeName, quantity
        );
        return "redirect:/Admin/tonkho";
    }

    @PostMapping("/addProvider")
    @ResponseBody
    @Transactional  // Đảm bảo lưu DB
    public Provider addProvider(@RequestBody Provider provider) {
        if (provider.getProviderName() == null || provider.getProviderName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên nhà cung cấp không được để trống");
        }
        return providerService.save(provider); // Trả về đối tượng có ID
    }

    @PostMapping("/addGenre")
    @ResponseBody
    @Transactional  
    public Genre addGenre(@RequestBody Map<String, String> body) {
        String genreName = body.get("genre");
        return genreService.saveGenre(genreName);
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