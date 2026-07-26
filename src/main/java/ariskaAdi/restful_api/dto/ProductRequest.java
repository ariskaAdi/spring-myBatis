package ariskaAdi.restful_api.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductRequest {

    private String name;
    private BigDecimal price;
    private Integer stock;
    
} 
