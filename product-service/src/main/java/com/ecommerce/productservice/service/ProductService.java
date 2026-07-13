package com.ecommerce.productservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional
    public Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    @Transactional
    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public Product update(String id, Product productDetails) {
        Product product = findById(id);
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setCategory(productDetails.getCategory());
        product.setImageUrl(productDetails.getImageUrl());
        return productRepository.save(product);
    }

    @Transactional
    public void delete(String id) {
        Product product = findById(id);
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Transactional
    public Product updateStock(String id, Integer quantity) {
        Product product = findById(id);
        product.setStockQuantity(quantity);
        return productRepository.save(product);
    }

    @Transactional
    public Product decrementStock(String id, Integer amount) {
        Product product = findById(id);

        int newStock = product.getStockQuantity() - amount;
        if (newStock < 0) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                            id, product.getStockQuantity(), amount)
            );
        }

        product.setStockQuantity(newStock);
        return productRepository.save(product);
    }
}
