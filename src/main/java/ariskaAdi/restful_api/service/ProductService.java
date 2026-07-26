package ariskaAdi.restful_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ariskaAdi.restful_api.dto.ProductResponse;
import ariskaAdi.restful_api.mapper.ProductMapper;
import ariskaAdi.restful_api.model.Product;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;

    public List<ProductResponse> findAll() {
        return productMapper.findAll().stream()
                .map(this::toResponse)
                .toList();       
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setPrice(product.getPrice());
        response.setStock(product.getStock());
        response.setCreatedAt(product.getCreatedAt());
        return response;
    }
}
