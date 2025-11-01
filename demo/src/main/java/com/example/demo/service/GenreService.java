// src/main/java/com/example/demo/service/GenreService.java
package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Genre;
import com.example.demo.repository.GenreRepository;

@Service
public class GenreService {

    @Autowired
    private GenreRepository genreRepo;

    public List<Genre> findAllGenres() {
        return genreRepo.findAll();
    }

    public Genre saveGenre(String genreName) {
        return genreRepo.findByGenreName(genreName)
            .orElseGet(() -> {
                Genre g = new Genre();
                g.setGenreName(genreName);
                return genreRepo.save(g);
            });
    }

    public Genre getByName(String name) {
        return genreRepo.findByGenreName(name).orElse(null);
    }
    public Optional<Genre> getById(Integer id) { 
    // Bỏ .orElse(null) và trả về trực tiếp Optional từ Repository
    return genreRepo.findByGenreId(id);
}
}
