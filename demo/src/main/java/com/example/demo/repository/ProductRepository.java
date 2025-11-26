package com.example.demo.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>{

    @Query("SELECT p FROM Product p WHERE p.productName = :name AND p.provider.providerId = :providerId")
    Optional<Product> findByProductNameAndProviderProviderId(@Param("name") String name, @Param("providerId") Integer providerId);
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.sizes WHERE p.provider.providerId = :providerId")
    List<Product> findByProviderIdWithSizes(@Param("providerId") Integer providerId);

    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.sizes " +
           "LEFT JOIN FETCH p.genre " +
           "LEFT JOIN FETCH p.provider")
    List<Product> findAllWithDetails();

    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.sizes " +
           "LEFT JOIN FETCH p.genre " +
           "LEFT JOIN FETCH p.provider")
    List<Product> findAllWithPrices();

    @Query("SELECT new map(" +
           "p.productName as productName, " +
           "p.genre.genreId as genreId, " +
           "p.basisPrice as basisPrice, " +
           "p.productId as productId) " +
           "FROM Product p WHERE p.provider.providerId = :providerId")
    List<Map<String, Object>> findSuggestionsByProvider(@Param("providerId") Integer providerId);

    // === [ĐÃ CẬP NHẬT] HỖ TRỢ PHÂN TRANG ===
    // Lưu ý: Đã bỏ "LEFT JOIN FETCH p.sizes" để tối ưu hóa query Count cho phân trang
    @Query("SELECT p FROM Product p " + 
           "LEFT JOIN FETCH p.genre g " +
           "LEFT JOIN FETCH p.provider pv " +
           "WHERE (:genreId IS NULL OR p.genre.genreId = :genreId) AND " +
           "(:brandId IS NULL OR p.provider.providerId = :brandId) AND " +
           "(:minPrice IS NULL OR p.sellPrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.sellPrice <= :maxPrice)")
    Page<Product> findFilteredProducts(
        @Param("genreId") Integer genreId,
        @Param("brandId") Integer brandId,
        @Param("minPrice") Float minPrice,
        @Param("maxPrice") Float maxPrice,
        Pageable pageable); // Thay Sort bằng Pageable
        
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.sizes WHERE p.genre.genreId = :genreId")
    List<Product> findByGenreGenreId(Integer genreId);
    
    List<Product> findFirst10ByProductNameContainingIgnoreCase(String keyword);

    List<Product> findFirst10ByGenreGenreIdAndProductNameContainingIgnoreCase(Integer genreId, String keyword);
    
}