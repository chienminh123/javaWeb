package com.example.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Product;
import com.example.demo.model.Provider;
import com.example.demo.model.Sizes;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProviderRepository;
import com.example.demo.repository.SizesRepository;

@Service
public class ProductService {

    @Autowired private ProductRepository productRepo;
    @Autowired private SizesRepository sizeRepo;
    @Autowired private ProviderRepository providerRepo;
    @Autowired private ImageService imageService;
    @Autowired private GenreService genreService;

    @Transactional
    public void saveMultipleProducts(
        String[] productNames, Integer[] providerIds, String[] genreNames,
        Float[] basisPrices, String[] descriptions, MultipartFile[][] images,
        String[] sizeNames, Integer[] quantities
    ) {
        int sizeIndex = 0;

        for (int i = 0; i < productNames.length; i++) {
            String name = productNames[i];
            Integer providerId = providerIds[i];
            String genreName = genreNames[i];
            Float basisPrice = basisPrices[i];
            String description = descriptions[i];

            // 1. Tìm hoặc tạo Product
            Product product = productRepo.findByProductNameAndProviderProviderId(name, providerId)
                .orElse(null);

            if (product == null) {
                product = new Product();
                product.setProductName(name);
                product.setProvider(providerRepo.findById(providerId).orElse(null));
                product.setGenre(genreService.saveGenre(genreName));
                product.setBasisPrice(basisPrice);
                product.setDescription(description);
            }

            // 2. Lưu để có ID
            product = productRepo.save(product);

            // 3. Lưu 1 ảnh chính
            if (images[i] != null && images[i].length > 0) {
                MultipartFile mainImage = images[i][0];
                String imageUrl = imageService.saveSingleImage(mainImage, product.getProductId());
                product.setImageUrl(imageUrl);
                product = productRepo.save(product);
            }

            // 4. Xử lý size
            while (sizeIndex < sizeNames.length 
                && sizeNames[sizeIndex] != null 
                && !sizeNames[sizeIndex].trim().isEmpty()) {

                String currentSizeName = sizeNames[sizeIndex];
                Integer currentQuantity = quantities[sizeIndex];

                Sizes size = sizeRepo.findByProductAndSizeName(product, currentSizeName)
                    .orElse(null);

                if (size == null) {
                    size = new Sizes();
                    size.setProduct(product);
                    size.setSizeName(currentSizeName);
                    size.setQantity(0);
                }

                size.setQantity(size.getQantity() + currentQuantity);
                sizeRepo.save(size);
                sizeIndex++;
            }
        }
    }

    public Map<Integer, List<Map<String, Object>>> getAllProductSuggestionsMap() {
        Map<Integer, List<Map<String, Object>>> map = new HashMap<>();
        List<Provider> providers = providerRepo.findAll();
        for (Provider p : providers) {
            map.put(p.getProviderId(), productRepo.findSuggestionsByProvider(p.getProviderId()));
        }
        return map;
    }
}