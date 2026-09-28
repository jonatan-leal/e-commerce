package com.ecommerce.productservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.ecommerce.productservice.model.Product;

public interface ProductRepository extends JpaRepository<Product, String> {
    
    List<Product> findByCategory(String category);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0")
    List<Product> findAvailableProducts();

    List<Product> findByNameContainingIgnoreCase(String name);

}
