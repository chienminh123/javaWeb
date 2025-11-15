package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Genre;
import com.example.demo.model.InventoryCheck;
import com.example.demo.model.InventoryDetail;
import com.example.demo.model.OrderDetail;
import com.example.demo.model.Orders;
import com.example.demo.model.Product;
import com.example.demo.model.Provider;
import com.example.demo.model.Quittance;
import com.example.demo.model.Sizes;
import com.example.demo.repository.InventoryCheckRepository;
import com.example.demo.repository.InventoryDetailRepository;
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
    @Autowired private InventoryCheckRepository checkRepo;
    @Autowired private InventoryDetailRepository detailRepo;

   @Transactional
    public void saveMultipleProducts(
    String[] productNames, Integer[] providerIds, Integer[] genreIds,
        Float[] basisPrices, String[] descriptions, MultipartFile[][] images,
       
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
//Inventory
    @Transactional
    public void saveInventoryCheck(
       
        Integer[] productId,
        String[] sizeName,
        Integer[] systemQty,
        Integer[] actualQty,
        String[] note
    ) {
        // 1. Tạo phiếu kiểm kê
        InventoryCheck check = new InventoryCheck();
        check.setCheckDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        
        check = checkRepo.save(check);

        // 2. Lưu từng chi tiết + cập nhật tồn
        for (int i = 0; i < productId.length; i++) {
            
            // === TẠO BIẾN FINAL ĐỂ SỬA LỖI ===
            final int currentProductId = productId[i];
            final String currentSizeName = sizeName[i];
            // === KẾT THÚC SỬA LỖI ===

            Product product = productRepo.findById(currentProductId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm ID " + currentProductId + " không tồn tại"));

            Sizes size = sizeRepo.findByProductAndSizeName(product, currentSizeName)
                .orElseThrow(() -> new RuntimeException("Size '" + currentSizeName + "' của sản phẩm '" + product.getProductName() + "' không tồn tại"));

            InventoryDetail detail = new InventoryDetail();
            detail.setInventoryCheck(check);
            detail.setProduct(size.getProduct());
            detail.setSize(size); // Lưu lại size
            detail.setSystemQuantity(systemQty[i]);
            detail.setActualQuantity(actualQty[i]);
            detail.setDifference(actualQty[i] - systemQty[i]);
            detail.setNote(note[i]);
            detailRepo.save(detail);

            // CẬP NHẬT TỒN THỰC TẾ
            size.setQuantity(actualQty[i]);
            sizeRepo.save(size);
        }
    }
@Transactional
    public String updateSingleProduct(
            Integer productId, Integer providerId, Integer genreId,
            String productName, Float basisPrice, Float markupPercent,
            String description, MultipartFile imageFile
    ) {
        // 1. Tìm sản phẩm
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

        // 2. Tìm các đối tượng liên quan
        Provider provider = providerRepo.findById(providerId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy NCC với ID: " + providerId));
        
        // === SỬA LỖI CŨ CỦA BẠN (getById -> findById) ===
        Genre genre = genreService.getById(genreId) // Giả định bạn đã có hàm findById trả về Optional
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại với ID: " + genreId));

        // 3. Cập nhật thông tin
        product.setProductName(productName);
        product.setProvider(provider);
        product.setGenre(genre);
        product.setBasisPrice(basisPrice);
        product.setDescription(description);

        // 4. Tính giá bán nếu có % markup
        if (markupPercent != null && markupPercent > 0) {
            product.setSellPrice(basisPrice * (1 + markupPercent / 100));
        } else {
             // Nếu người dùng xóa % markup, ta giữ lại giá bán cũ
             // (Hoặc bạn có thể set = basisPrice tùy logic)
            product.setSellPrice(product.getSellPrice());
        }

        // 5. Xử lý ảnh (nếu có ảnh mới)
        String newImageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            newImageUrl = imageService.saveSingleImage(imageFile, productId);
            product.setImageUrl(newImageUrl);
        }

        // 6. Lưu vào DB
        productRepo.save(product);

        return newImageUrl; // Trả về URL ảnh mới để JS cập nhật
    }
    public double calculateTotalInventoryValue() {
        // Lấy tất cả sản phẩm (bao gồm cả size)
        List<Product> allProducts = productRepo.findAllWithDetails(); 
        double totalValue = 0.0;

        for (Product p : allProducts) {
            if (p.getSizes() != null) {
                for (Sizes s : p.getSizes()) {
                    if (s.getQuantity() != null && s.getQuantity() > 0) {
                        totalValue += (p.getBasisPrice() * s.getQuantity());
                    }
                }
            }
        }
        return totalValue;
    }

    public long countOutOfStockProducts() {
        List<Product> allProducts = productRepo.findAllWithDetails();
        long outOfStockCount = 0;

        for (Product p : allProducts) {
            if (p.getSizes() == null || p.getSizes().isEmpty()) {
                outOfStockCount++; // Không có size = hết hàng
                continue;
            }

            // Kiểm tra xem có size nào CÒN HÀNG không
            boolean anyInStock = p.getSizes().stream()
                .anyMatch(s -> s.getQuantity() != null && s.getQuantity() > 0);
            
            if (!anyInStock) {
                outOfStockCount++; // Nếu không có size nào còn hàng = hết hàng
            }
        }
        return outOfStockCount;
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
        return productRepo.findAllWithDetails(); 
    }
    
    // public List<Product> findProductsByGenre(Integer genreId) {
    //     return productRepo.findByGenreGenreId(genreId);
    // }
    public List<Product> findProductsByGenre(
        Integer genreId, String sortParam, String priceRange, Integer brandId) {
        
        // 1. Xây dựng Sort
        Sort sorting = Sort.unsorted();
        if ("price_asc".equals(sortParam)) {
            sorting = Sort.by(Sort.Direction.ASC, "sellPrice");
        } else if ("price_desc".equals(sortParam)) {
            sorting = Sort.by(Sort.Direction.DESC, "sellPrice");
        } else {
             // Sắp xếp mặc định theo ID mới nhất
             sorting = Sort.by(Sort.Direction.DESC, "productId");
        }
        
        // 2. Xử lý Khoảng giá (Phân tích chuỗi 'min-max')
        Float minPrice = null;
        Float maxPrice = null;
        if (priceRange != null && !priceRange.isEmpty()) {
            String[] parts = priceRange.split("-");
            try {
                if (parts.length > 0 && !parts[0].equalsIgnoreCase("min")) {
                    minPrice = Float.parseFloat(parts[0]);
                }
                if (parts.length > 1 && !parts[1].equalsIgnoreCase("max")) {
                    // Nếu giá trị là 'max', ta để maxPrice là null (không giới hạn trên)
                    if (!parts[1].equalsIgnoreCase("max")) {
                       maxPrice = Float.parseFloat(parts[1]);
                    }
                }
            } catch (NumberFormatException e) {
                // Bỏ qua nếu giá trị không hợp lệ
            }
        }
        
        // 3. Gọi hàm Repository mới
        return productRepo.findFilteredProducts(
            genreId, brandId, minPrice, maxPrice, sorting);
    }
    public Optional<Product> findById(Integer id) {
        return productRepo.findById(id);
    }

public List<Product> searchSuggestions(String keyword, Integer genreId) {
    if (keyword == null || keyword.trim().isEmpty()) {
        return List.of(); // Trả về danh sách rỗng
    }
    String trimmedKeyword = keyword.trim();
    
    if (genreId != null) {
        return productRepo.findFirst10ByGenreGenreIdAndProductNameContainingIgnoreCase(genreId, trimmedKeyword);
    } else {
        return productRepo.findFirst10ByProductNameContainingIgnoreCase(trimmedKeyword);
    }
}

/**
     * Kiểm tra tồn kho cho các sản phẩm trong đơn hàng vừa tạo.
     */
    public List<Sizes> checkLowStockAfterOrder(Orders order, int threshold) {
        if (order.getOrderDetails() == null) {
            return List.of();
        }
        
        return order.getOrderDetails().stream()
            // Lấy đối tượng Sizes từ OrderDetails 
            .map(OrderDetail::getSizes) 
            .filter(size -> size != null && size.getQuantity() != null && size.getQuantity() < threshold)
            .toList();
    }
}