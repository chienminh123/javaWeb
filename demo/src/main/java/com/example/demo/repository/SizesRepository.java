package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import com.example.demo.model.Product;
import com.example.demo.model.Sizes;


@Repository
public interface SizesRepository extends JpaRepository<Sizes, Integer> {

    Optional<Sizes> findByProductAndSizeName(Product product, String sizeName);
}

