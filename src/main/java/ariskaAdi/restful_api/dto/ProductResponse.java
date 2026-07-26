package ariskaAdi.restful_api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime createdAt;
    
}
