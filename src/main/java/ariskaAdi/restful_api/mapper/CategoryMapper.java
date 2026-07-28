package ariskaAdi.restful_api.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import ariskaAdi.restful_api.model.Category;

@Mapper
public interface CategoryMapper {
    Optional<Category> findWithProducts(Long id);
}
