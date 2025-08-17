package com.techoworld.eightythree.repository;

import com.techoworld.eightythree.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {}