package com.techoworld.eightythree.service;

import com.techoworld.eightythree.entity.Product;
import com.techoworld.eightythree.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductRepository repo;
    public ProductService(ProductRepository repo) { this.repo = repo; }

    public List<Product> findAll() { return repo.findAll(); }
    public Optional<Product> findById(Long id) { return repo.findById(id); }
    public Product save(Product p) { return repo.save(p); }
    public void deleteById(Long id) { repo.deleteById(id); }
}