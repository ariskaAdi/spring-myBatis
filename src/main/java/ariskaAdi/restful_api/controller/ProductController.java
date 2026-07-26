package ariskaAdi.restful_api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ariskaAdi.restful_api.mapper.ProductMapper;
import ariskaAdi.restful_api.model.Product;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductMapper productMapper;

    @GetMapping
    public List<Product> getAll() {
        return productMapper.findAll();
    }

}
