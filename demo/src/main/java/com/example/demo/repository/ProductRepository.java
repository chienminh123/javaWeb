package com.example.demo.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
@Query("SELECT new map(" +
       "p.productName as productName, " +
       "p.genre.genreId as genreId, " +
       "p.basisPrice as basisPrice, " +
       "p.productId as productId) " +
       "FROM Product p WHERE p.provider.providerId = :providerId")
List<Map<String, Object>> findSuggestionsByProvider(@Param("providerId") Integer providerId);
}