package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.PageRequest; // Import PageRequest
import org.springframework.data.domain.Pageable; // Import Pageable
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
import com.example.demo.repository.OrderDetailRepository;
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
    @Autowired private OrderDetailRepository orderDetailRepo;
  

    @Transactional
    public void saveMultipleProducts(
        String[] productNames, Integer[] providerIds, Integer[] genreIds,
        Float[] basisPrices, String[] descriptions, MultipartFile[][] images,
        String[] sizeNames, String[] quantities
    ) {
        Map<Integer, Quittance> providerQuittanceMap = new HashMap<>();

        for (int i = 0; i < productNames.length; i++) {
            String name = productNames[i].trim();
            if (name.isEmpty()) continue;

            final Integer currentProviderId = providerIds[i];
            final Integer currentGenreId = genreIds[i];
            final Float currentBasisPrice = basisPrices[i];
            final String currentDescription = descriptions[i];

            Quittance quittance = providerQuittanceMap.computeIfAbsent(currentProviderId, id -> {
                Provider provider = providerRepo.findById(id).orElse(null);
                Quittance q = new Quittance();
                q.setQuittanceName("Nhập kho từ " + (provider != null ? provider.getProviderName() : "Không xác định") + " - " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                q.setDate(LocalDateTime.now());
                q.setNote("Nhập kho tự động từ form thêm sản phẩm");
                q.setQuittanceType("IMPORT");
                q.setProvider(provider);
                return q;
            });

            final Product product = productRepo.findByProductNameAndProviderProviderId(name, currentProviderId)
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setProductName(name);
                    p.setProvider(providerRepo.findById(currentProviderId).orElseThrow(() -> new IllegalArgumentException("NCC không tồn tại")));
                    p.setGenre(genreService.getById(currentGenreId).orElseThrow(() -> new IllegalArgumentException("Thể loại không tồn tại")));
                    p.setBasisPrice(currentBasisPrice);
                    p.setDescription(currentDescription);
                    p.setSellPrice(currentBasisPrice * 1.5f);
                    return p;
                });

            Product savedProduct = productRepo.save(product);

            if (images != null && images.length > i && images[i] != null && images[i].length > 0) {
                String imageUrl = imageService.saveSingleImage(images[i][0], savedProduct.getProductId());
                savedProduct.setImageUrl(imageUrl);
                productRepo.save(savedProduct);
            }

            boolean hasValidSize = false;
            if (sizeNames != null && sizeNames.length > i && quantities != null && quantities.length > i) {
            
            String rawSizes = sizeNames[i];
            String rawQties = quantities[i];

            if (rawSizes != null && !rawSizes.isEmpty()) {
                String[] listSizes = rawSizes.split(",");
                String[] listQties = rawQties.split(",");

                for (int j = 0; j < listSizes.length; j++) {
                    String sizeNameVal = listSizes[j].trim();
                    String qtyStr = (j < listQties.length) ? listQties[j] : "0";
                    Integer qty = 0;
                    try {
                        qty = Integer.parseInt(qtyStr);
                    } catch (NumberFormatException e) { qty = 0; }

                    if (sizeNameVal.isEmpty() || qty <= 0) continue;

                    hasValidSize = true;
                    final String sName = sizeNameVal; 
                    Sizes size = sizeRepo.findByProductAndSizeName(savedProduct, sName)
                        .orElseGet(() -> {
                            Sizes s = new Sizes();
                            s.setProduct(savedProduct);
                            s.setSizeName(sName);
                            s.setQuantity(0);
                            return s;
                        });

                    size.setQuantity(size.getQuantity() + qty);
                    sizeRepo.save(size);

                    String note = quittance.getNote() + "\n" + savedProduct.getProductName() + " - " + sizeNameVal + " x" + qty;
                    quittance.setNote(note.trim());
                }
            }
        }

        if (!hasValidSize) throw new IllegalArgumentException("Sản phẩm '" + name + "' cần ít nhất 1 size có số lượng");
        quittance.setProduct(savedProduct);
    }

    if (!providerQuittanceMap.isEmpty()) quittanceRepo.saveAll(providerQuittanceMap.values());
}

@Transactional
    public void deleteProduct(Integer productId) {
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        boolean hasOrders = orderDetailRepo.existsByProduct(product);
        if (hasOrders) {
             throw new RuntimeException("Sản phẩm đang nằm trong đơn hàng, không thể xóa! Hãy đặt số lượng về 0 để ẩn.");
        }

        productRepo.deleteById(productId);
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
                Provider provider = providerRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("NCC ID " + id + " không tồn tại."));
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

            if (currentSizeNames == null || currentQuantities == null) throw new IllegalArgumentException("Sản phẩm lỗi");

            boolean hasValidSize = false;
            for (int j = 0; j < currentSizeNames.length; j++) {
                String sizeName = currentSizeNames[j];
                Integer exportQty = currentQuantities[j];

                if (sizeName == null || sizeName.trim().isEmpty() || exportQty == null || exportQty <= 0) continue;
                
                hasValidSize = true;
                final String finalSizeName = sizeName.trim();

                Sizes size = sizeRepo.findByProductAndSizeName(product, finalSizeName)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không có size này"));

                int currentStock = size.getQuantity();
                if (currentStock < exportQty) {
                    throw new IllegalStateException("Không đủ hàng!");
                }

                size.setQuantity(currentStock - exportQty);
                sizeRepo.save(size);

                String note = quittance.getNote() + "\n- " + product.getProductName() + " (Size: " + finalSizeName + ") x " + exportQty;
                quittance.setNote(note.trim());
            }

            if (!hasValidSize) throw new IllegalArgumentException("Cần ít nhất 1 size");
            quittance.setProduct(product);
        }

        if (!providerQuittanceMap.isEmpty()) quittanceRepo.saveAll(providerQuittanceMap.values());
    }

    @Transactional
    public void saveInventoryCheck(Integer[] productId, String[] sizeName, Integer[] systemQty, Integer[] actualQty, String[] note) {
        InventoryCheck check = new InventoryCheck();
        check.setCheckDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        check = checkRepo.save(check);

        for (int i = 0; i < productId.length; i++) {
            final int currentProductId = productId[i];
            final String currentSizeName = sizeName[i];

            Product product = productRepo.findById(currentProductId).orElseThrow(() -> new RuntimeException("SP không tồn tại"));
            Sizes size = sizeRepo.findByProductAndSizeName(product, currentSizeName).orElseThrow(() -> new RuntimeException("Size không tồn tại"));

            InventoryDetail detail = new InventoryDetail();
            detail.setInventoryCheck(check);
            detail.setProduct(size.getProduct());
            detail.setSize(size);
            detail.setSystemQuantity(systemQty[i]);
            detail.setActualQuantity(actualQty[i]);
            detail.setDifference(actualQty[i] - systemQty[i]);
            detail.setNote(note[i]);
            detailRepo.save(detail);

            size.setQuantity(actualQty[i]);
            sizeRepo.save(size);
        }
    }

    @Transactional
    public String updateSingleProduct(
            Integer productId, Integer providerId, Integer genreId,
            String productName, Float basisPrice, Float markupPercent,Integer discount,
            String description, MultipartFile imageFile
    ) {
        Product product = productRepo.findById(productId).orElseThrow(() -> new RuntimeException("Lỗi"));
        Provider provider = providerRepo.findById(providerId).orElseThrow(() -> new RuntimeException("Lỗi"));
        Genre genre = genreService.getById(genreId).orElseThrow(() -> new RuntimeException("Lỗi"));

        product.setProductName(productName);
        product.setProvider(provider);
        product.setGenre(genre);
        product.setBasisPrice(basisPrice);
        product.setDiscount(discount != null ? discount : 0);
        product.setMarkupPercent(markupPercent != null ? markupPercent : 0f);
        product.setDescription(description);

        if (markupPercent != null && markupPercent > 0) {
            product.setSellPrice(basisPrice * (1 + markupPercent / 100));
        }

        String newImageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            newImageUrl = imageService.saveSingleImage(imageFile, productId);
            product.setImageUrl(newImageUrl);
        }
        productRepo.save(product);
        return newImageUrl;
    }

    public double calculateTotalInventoryValue() {
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
                outOfStockCount++;
                continue;
            }
            boolean anyInStock = p.getSizes().stream().anyMatch(s -> s.getQuantity() != null && s.getQuantity() > 0);
            if (!anyInStock) outOfStockCount++;
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
    
    public Page<Product> findProductsByGenreWithPagination(
        Integer genreId, String sortParam, String priceRange, Integer brandId, int page, int pageSize) {
        
        Sort sorting = Sort.unsorted();
        if ("price_asc".equals(sortParam)) {
            sorting = Sort.by(Sort.Direction.ASC, "sellPrice");
        } else if ("price_desc".equals(sortParam)) {
            sorting = Sort.by(Sort.Direction.DESC, "sellPrice");
        } else {
            sorting = Sort.by(Sort.Direction.DESC, "productId");
        }

        Float minPrice = null;
        Float maxPrice = null;
        if (priceRange != null && !priceRange.isEmpty()) {
            String[] parts = priceRange.split("-");
            try {
                if (parts.length > 0 && !parts[0].equalsIgnoreCase("min")) {
                    minPrice = Float.parseFloat(parts[0]);
                }
                if (parts.length > 1 && !parts[1].equalsIgnoreCase("max")) {
                    maxPrice = Float.parseFloat(parts[1]);
                }
            } catch (NumberFormatException e) {
            }
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize, sorting);

        return productRepo.findFilteredProducts(genreId, brandId, minPrice, maxPrice, pageable);
    }

    public List<Product> findProductsByGenre(
        Integer genreId, String sortParam, String priceRange, Integer brandId) {
        
        Page<Product> page = findProductsByGenreWithPagination(
            genreId, sortParam, priceRange, brandId, 1, Integer.MAX_VALUE);
            
        return page.getContent();
    }
    
    public Optional<Product> findById(Integer id) {
        return productRepo.findById(id);
    }

    public List<Product> searchSuggestions(String keyword, Integer genreId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String trimmedKeyword = keyword.trim();
        
        if (genreId != null) {
            return productRepo.findFirst10ByGenreGenreIdAndProductNameContainingIgnoreCase(genreId, trimmedKeyword);
        } else {
            return productRepo.findFirst10ByProductNameContainingIgnoreCase(trimmedKeyword);
        }
    }

    public List<Sizes> checkLowStockAfterOrder(Orders order, int threshold) {
        if (order.getOrderDetails() == null) return List.of();
        return order.getOrderDetails().stream()
            .map(OrderDetail::getSizes) 
            .filter(size -> size != null && size.getQuantity() != null && size.getQuantity() < threshold)
            .toList();
    }
}