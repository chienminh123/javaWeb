package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Genre;
import com.example.demo.model.Product;
import com.example.demo.model.Provider;
import com.example.demo.model.Quittance;
import com.example.demo.model.Sizes;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProviderRepository;
import com.example.demo.repository.QuittanceRepository;
import com.example.demo.repository.SizesRepository;

@Service
public class ProductService {

    @Autowired private ProductRepository productRepo;
    @Autowired private SizesRepository sizeRepo;
    @Autowired private ProviderRepository providerRepo;
    @Autowired private GenreService genreService;
    @Autowired private ImageService imageService;
    @Autowired private QuittanceRepository quittanceRepo; 

   @Transactional
public void saveMultipleProducts(
    String[] productNames, Integer[] providerIds, Integer[] genreIds,
        Float[] basisPrices, String[] descriptions, MultipartFile[][] images,
        // === THAY ĐỔI Ở ĐÂY: Sửa signature để nhận mảng 2 chiều ===
        String[][] sizeNames, Integer[][] quantities
) {
    Map<Integer, Quittance> providerQuittanceMap = new HashMap<>();

    for (int i = 0; i < productNames.length; i++) {
            String name = productNames[i].trim();
            if (name.isEmpty()) continue;

            // --- BIẾN FINAL ĐỂ DÙNG TRONG LAMBDA ---
            final Integer currentProviderId = providerIds[i];
            final Integer currentGenreId = genreIds[i];
            final Float currentBasisPrice = basisPrices[i];
            final String currentDescription = descriptions[i];

        // --- TẠO BIÊN LAI THEO NCC ---
            Quittance quittance = providerQuittanceMap.computeIfAbsent(currentProviderId, id -> {
            Provider provider = providerRepo.findById(id).orElse(null);
            // Product product = productRepo.findByProductNameAndProviderProviderId(name, currentProviderId).orElse(null);
            String providerName = provider != null ? provider.getProviderName() : "Không xác định";

            Quittance q = new Quittance();
            q.setQuittanceName("Nhập kho từ " + providerName + " - " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            q.setDate(LocalDateTime.now());
            q.setNote("Nhập kho tự động từ form thêm sản phẩm");
            q.setQuittanceType("IMPORT"); // ĐÁNH DẤU ĐÂY LÀ BIÊN LAI NHẬP
            q.setProvider(provider);
            // q.setProduct(product);
            return q;
        });

        // --- TÌM HOẶC TẠO PRODUCT ---
        final Product product = productRepo.findByProductNameAndProviderProviderId(name, currentProviderId)
            .orElseGet(() -> {
                Product p = new Product();
                p.setProductName(name);

                Provider provider = providerRepo.findById(currentProviderId)
                    .orElseThrow(() -> new IllegalArgumentException("NCC không tồn tại: " + currentProviderId));
                p.setProvider(provider);

                Genre genre = genreService.getById(currentGenreId)
                    .orElseThrow(() -> new IllegalArgumentException("Thể loại không tồn tại: " + currentGenreId));
                p.setGenre(genre);

                p.setBasisPrice(currentBasisPrice);
                p.setDescription(currentDescription);
                p.setSellPrice(currentBasisPrice * 1.5f); // giá bán = 150% giá gốc
                return p;
            });

        // Lưu product để có ID
        Product savedProduct = productRepo.save(product);

        // --- LƯU ẢNH ---
        if (images != null && images.length > i && images[i] != null && images[i].length > 0) {
            String imageUrl = imageService.saveSingleImage(images[i][0], savedProduct.getProductId());
            savedProduct.setImageUrl(imageUrl);
            productRepo.save(savedProduct);
        }

        // --- XỬ LÝ SIZE ---
        boolean hasValidSize = false;
            if (sizeNames != null && sizeNames.length > i && quantities != null && quantities.length > i) {
                String[] currentProductSizeNames = sizeNames[i];
                Integer[] currentProductQuantities = quantities[i];

                for (int j = 0; j < currentProductSizeNames.length; j++) {
                    String sizeNameVal = currentProductSizeNames[j] != null ? currentProductSizeNames[j].trim() : "";
                    Integer qty = currentProductQuantities[j] != null ? currentProductQuantities[j] : 0;

                    // Bỏ qua nếu tên size rỗng hoặc số lượng <= 0
                    if (sizeNameVal.isEmpty() || qty <= 0) {
                        continue;
                    }

                    hasValidSize = true; // Đánh dấu là đã tìm thấy size hợp lệ

                    Sizes size = sizeRepo.findByProductAndSizeName(savedProduct, sizeNameVal)
                        .orElseGet(() -> {
                            Sizes s = new Sizes();
                            s.setProduct(savedProduct);
                            s.setSizeName(sizeNameVal);
                            s.setQuantity(0); // Khởi tạo số lượng là 0
                            return s;
                        });

                    size.setQuantity(size.getQuantity() + qty); // Cộng dồn số lượng
                    sizeRepo.save(size);

                    // Cập nhật note biên lai
                    String note = quittance.getNote() + "\n" +
                        savedProduct.getProductName() + " - " + sizeNameVal + " x" + qty;
                    quittance.setNote(note.trim());
                }
            }

        if (!hasValidSize) {
            throw new IllegalArgumentException("Sản phẩm '" + name + "' cần ít nhất 1 size có số lượng");
        }

        quittance.setProduct(savedProduct);
    }

    // Lưu biên lai
    if (!providerQuittanceMap.isEmpty()) {
        quittanceRepo.saveAll(providerQuittanceMap.values());
    }
}
    @Transactional
    public void exportMultipleProducts(
            Integer[] providerIds, Integer[] productIds, 
            String[][] sizeNames, Integer[][] quantities)
            throws IllegalStateException, IllegalArgumentException {

        Map<Integer, Quittance> providerQuittanceMap = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (int i = 0; i < productIds.length; i++) {
            final Integer currentProviderId = providerIds[i];
            final Integer currentProductId = productIds[i];
            
            if (currentProviderId == null || currentProductId == null) continue;

            Product product = productRepo.findById(currentProductId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm ID " + currentProductId + " không tồn tại."));

            Quittance quittance = providerQuittanceMap.computeIfAbsent(currentProviderId, id -> {
                Provider provider = providerRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("NCC ID " + id + " không tồn tại."));
                
                Quittance q = new Quittance();
                q.setQuittanceName("Phiếu xuất kho - " + provider.getProviderName() + " - " + LocalDateTime.now().format(formatter));
                q.setDate(LocalDateTime.now());
                q.setQuittanceType("EXPORT"); 
                q.setProvider(provider);      
                q.setNote("Chi tiết xuất kho:");
                return q;
            });


            String[] currentSizeNames = sizeNames[i];
            Integer[] currentQuantities = quantities[i];

            if (currentSizeNames == null || currentQuantities == null) {
                 throw new IllegalArgumentException("Sản phẩm '" + product.getProductName() + "' phải có ít nhất 1 size để xuất.");
            }

            boolean hasValidSize = false;
            for (int j = 0; j < currentSizeNames.length; j++) {
                String sizeName = currentSizeNames[j];
                Integer exportQty = currentQuantities[j];

                if (sizeName == null || sizeName.trim().isEmpty() || exportQty == null || exportQty <= 0) {
                    continue;
                }
                
                hasValidSize = true;
                
                final String finalSizeName = sizeName.trim();

                // 2. Dùng biến 'finalSizeName' cho tất cả logic bên dưới
                Sizes size = sizeRepo.findByProductAndSizeName(product, finalSizeName)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Sản phẩm '" + product.getProductName() + "' không có size '" + finalSizeName + "'."));

                int currentStock = size.getQuantity();
                if (currentStock < exportQty) {
                    throw new IllegalStateException(
                        "Không đủ hàng! SP '" + product.getProductName() + 
                        "' (Size " + finalSizeName + ") chỉ còn " + currentStock + 
                        ", nhưng bạn muốn xuất " + exportQty);
                }

                size.setQuantity(currentStock - exportQty);
                sizeRepo.save(size);

                String note = quittance.getNote() + "\n- " + 
                    product.getProductName() + " (Size: " + finalSizeName + ") x " + exportQty;
                quittance.setNote(note.trim());
            }

            if (!hasValidSize) {
                throw new IllegalArgumentException("Sản phẩm '" + product.getProductName() + "' cần ít nhất 1 size có số lượng > 0 để xuất.");
            }
            quittance.setProduct(product);
        }

        if (!providerQuittanceMap.isEmpty()) {
            quittanceRepo.saveAll(providerQuittanceMap.values());
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
   public List<Product> getAllProductsWithInventory() {
    return productRepo.findAllWithDetails(); // JOIN FETCH tất cả
}
}