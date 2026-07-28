package ariskaAdi.restful_api.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ariskaAdi.restful_api.model.Product;

@Mapper
public interface ProductMapper {

    List<Product> search(@Param("name") String name,
                        @Param("categoryId") Long categoryId,
                        @Param("sortBy") String sortBy,
                        @Param("size") int size,
                        @Param("offset") int offset) ;
    
    Long count (@Param("name") String name, @Param("categoryId") Long categoryId);

    Optional<Product> findById(Long id);

    void insert(Product product);

    int update(Product product);

    int delete(Long id);
}
