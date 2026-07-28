package ariskaAdi.restful_api.model;

import java.util.List;

import lombok.Data;

@Data
public class Category {

    private Long id;
    private String name;
    private List<Product> products;
    
}
