package ariskaAdi.restful_api.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import ariskaAdi.restful_api.model.Product;

@Mapper
public interface ProductMapper {

    @Select("SELECT * FROM products ORDER BY id LIMIT #{size} OFFSET #{offset}")
    List<Product> findPage(int size, int offset);

    @Select("SELECT * FROM products WHERE id = #{id}")
    Optional<Product> findById(Long id);

    @Insert("INSERT INTO products (name, price, stock) VALUES (#{name}, #{price}, #{stock})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Product product);

    @Update("UPDATE products SET name=#{name}, price=#{price}, stock=#{stock} WHERE id=#{id}")
    int update(Product product);

    @Delete("DELETE FROM products WHERE id = #{id}")
    int delete(Long id);
}
