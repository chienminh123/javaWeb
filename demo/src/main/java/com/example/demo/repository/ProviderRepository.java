package com.example.demo.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Provider;
@Repository
public interface ProviderRepository extends JpaRepository<Provider, Integer> {
    @Query("SELECT DISTINCT p.provider FROM Product p WHERE p.genre.genreId = :genreId")
    List<Provider> findProvidersByGenre(@Param("genreId") Integer genreId);
}
