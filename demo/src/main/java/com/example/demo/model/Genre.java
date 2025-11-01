package com.example.demo.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class Genre {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Integer genreId;
private String genreName;
@JsonIgnore
@OneToMany(mappedBy = "genre")
private List<Product> products;
}
