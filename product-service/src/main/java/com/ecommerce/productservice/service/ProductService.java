package com.ecommerce.productservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.productservice.dto.ProductDto;
import com.ecommerce.productservice.exception.InsufficientStockException;
import com.ecommerce.productservice.mapper.ProductMapper;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductDto> findAll() {
        return productRepository.findAll().stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDto findById(String id) {
        return productMapper.toDto(getEntity(id));
    }

    @Transactional
    public ProductDto create(ProductDto productDto) {
        Product product = productMapper.toEntity(productDto);
        return productMapper.toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto update(String id, ProductDto productDto) {
        Product product = getEntity(id);
        product.setName(productDto.name());
        product.setDescription(productDto.description());
        product.setPrice(productDto.price());
        product.setStockQuantity(productDto.stockQuantity());
        product.setCategory(productDto.category());
        product.setImageUrl(productDto.imageUrl());
        return productMapper.toDto(productRepository.save(product));
    }

    @Transactional
    public void delete(String id) {
        productRepository.delete(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<ProductDto> findByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Transactional
    public ProductDto updateStock(String id, Integer quantity) {
        Product product = getEntity(id);
        product.setStockQuantity(quantity);
        return productMapper.toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto decrementStock(String id, Integer amount) {
        Product product = getEntity(id);

        int newStock = product.getStockQuantity() - amount;
        if (newStock < 0) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                            id, product.getStockQuantity(), amount));
        }

        product.setStockQuantity(newStock);
        return productMapper.toDto(productRepository.save(product));
    }

    private Product getEntity(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }
}