package com.ecommerce.productservice.mapper;

import com.ecommerce.productservice.dto.ProductDto;
import com.ecommerce.productservice.model.Product;
import org.springframework.stereotype.Component;

// Mapeo escrito a mano y no con MapStruct: con Lombok + MapStruct + JDK 25 el
// .class generado pierde "implements ProductMapper" de forma intermitente y
// Spring deja de encontrar el bean. Ver AGENTS.md (Known gaps).
@Component
public class ProductMapper {

    public ProductDto toDto(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCategory(),
                product.getImageUrl(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public Product toEntity(ProductDto productDto) {
        if (productDto == null) {
            return null;
        }
        return Product.builder()
                .id(productDto.id())
                .name(productDto.name())
                .description(productDto.description())
                .price(productDto.price())
                // defaultValue = "0" en el mapper de MapStruct anterior
                .stockQuantity(productDto.stockQuantity() != null ? productDto.stockQuantity() : 0)
                .category(productDto.category())
                .imageUrl(productDto.imageUrl())
                .build();
    }
}
