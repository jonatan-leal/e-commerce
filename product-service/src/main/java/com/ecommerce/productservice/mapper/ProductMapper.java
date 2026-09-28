package com.ecommerce.productservice.mapper;

import com.ecommerce.productservice.dto.ProductDto;
import com.ecommerce.productservice.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {

    ProductDto toDto(Product product);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "stockQuantity", defaultValue = "0")
    Product toEntity(ProductDto productDto);
}