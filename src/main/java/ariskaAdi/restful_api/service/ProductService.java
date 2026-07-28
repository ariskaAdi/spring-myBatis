package ariskaAdi.restful_api.service;


import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import ariskaAdi.restful_api.dto.ProductRequest;
import ariskaAdi.restful_api.dto.ProductResponse;
import ariskaAdi.restful_api.mapper.ProductMapper;
import ariskaAdi.restful_api.model.Product;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;

    public ProductResponse findById(Long id) {
        Product product = productMapper.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Product" + id + "not found"));
        return toResponse(product);
    }

    public List<ProductResponse> findPage(int page, int size) {
        int offset = page * size;
        return productMapper.search(null, null, null, size, offset).stream()
            .map(this::toResponse)
            .toList();
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        productMapper.insert(product);
        return toResponse(productMapper.findById(product.getId()).orElseThrow());
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productMapper.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Product" + id + " not found"));
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        productMapper.update(product);
        return toResponse(product);
    }

    public void delete(Long id) {
        productMapper.delete(id);
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
