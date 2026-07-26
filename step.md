# Step-by-Step Dummy Code — Product Catalog API

Companion to [todos.md](todos.md). Same phases, same order — but every step here has copy-pasteable code so you don't get lost.

Domain: **Product Catalog** — `products` belong to `categories`. Base package used below: `ariskaAdi.restful_api` (matches your existing `RestfulApiApplication`).

Do not skip ahead — later phases assume earlier files exist and delete/replace things from earlier phases (e.g. Phase 5 replaces the annotation-based mapper from Phase 1 with an XML mapper).

---

## Phase 0 — Environment Check

```bash
./mvnw clean install

docker run --name pg-restful \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=restful_api \
  -p 5432:5432 -d postgres:16
```

No app code yet. Confirm you can connect: `psql -h localhost -U postgres -d restful_api` (password `postgres`).

---

## Phase 1 — Hello Database

**`src/main/resources/application.properties`**
```properties
spring.application.name=restful-api

spring.datasource.url=jdbc:postgresql://localhost:5432/restful_api
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

mybatis.configuration.map-underscore-to-camel-case=true
```

**SQL — run manually via psql/DBeaver for now (Flyway takes over in Phase 6):**
```sql
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO products (name, price, stock) VALUES
  ('Keyboard', 49.99, 100),
  ('Mouse', 19.99, 200),
  ('Monitor', 199.99, 50);
```

**`src/main/java/ariskaAdi/restful_api/model/Product.java`**
```java
package ariskaAdi.restful_api.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime createdAt;
}
```

**`src/main/java/ariskaAdi/restful_api/mapper/ProductMapper.java`**
```java
package ariskaAdi.restful_api.mapper;

import ariskaAdi.restful_api.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("SELECT * FROM products")
    List<Product> findAll();
}
```

**`src/main/java/ariskaAdi/restful_api/controller/ProductController.java`**
```java
package ariskaAdi.restful_api.controller;

import ariskaAdi.restful_api.mapper.ProductMapper;
import ariskaAdi.restful_api.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductMapper productMapper; // direct call, just for this phase

    @GetMapping
    public List<Product> getAll() {
        return productMapper.findAll();
    }
}
```

Run the app, hit `GET http://localhost:8080/api/products` — you should see the 3 seeded rows.

---

## Phase 2 — Layered Architecture + DTOs

**`src/main/java/ariskaAdi/restful_api/dto/ProductRequest.java`**
```java
package ariskaAdi.restful_api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    private String name;
    private BigDecimal price;
    private Integer stock;
}
```

**`src/main/java/ariskaAdi/restful_api/dto/ProductResponse.java`**
```java
package ariskaAdi.restful_api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime createdAt;
}
```

**`src/main/java/ariskaAdi/restful_api/service/ProductService.java`**
```java
package ariskaAdi.restful_api.service;

import ariskaAdi.restful_api.dto.ProductRequest;
import ariskaAdi.restful_api.dto.ProductResponse;
import ariskaAdi.restful_api.mapper.ProductMapper;
import ariskaAdi.restful_api.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
```

**`ProductController.java` — updated to go through the service**
```java
package ariskaAdi.restful_api.controller;

import ariskaAdi.restful_api.dto.ProductResponse;
import ariskaAdi.restful_api.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> getAll() {
        return productService.findAll();
    }
}
```

Controller now never touches the mapper directly — that's the rule from here on.

---

## Phase 3 — Full CRUD + REST Conventions

**Add to `ProductMapper.java`:**
```java
package ariskaAdi.restful_api.mapper;

import ariskaAdi.restful_api.model.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

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
```

**`ProductService.java` — add CRUD methods:**
```java
public ProductResponse findById(Long id) {
    Product product = productMapper.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Product " + id + " not found"));
    return toResponse(product);
}

public List<ProductResponse> findPage(int page, int size) {
    int offset = page * size;
    return productMapper.findPage(size, offset).stream()
            .map(this::toResponse)
            .toList();
}

public ProductResponse create(ProductRequest request) {
    Product product = new Product();
    product.setName(request.getName());
    product.setPrice(request.getPrice());
    product.setStock(request.getStock());
    productMapper.insert(product); // populates generated id
    return toResponse(productMapper.findById(product.getId()).orElseThrow());
}

public ProductResponse update(Long id, ProductRequest request) {
    Product product = productMapper.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Product " + id + " not found"));
    product.setName(request.getName());
    product.setPrice(request.getPrice());
    product.setStock(request.getStock());
    productMapper.update(product);
    return toResponse(product);
}

public void delete(Long id) {
    productMapper.delete(id);
}
```
(NoSuchElementException is a placeholder — replaced by a proper custom exception in Phase 4.)

**`ProductController.java` — full CRUD:**
```java
package ariskaAdi.restful_api.controller;

import ariskaAdi.restful_api.dto.ProductRequest;
import ariskaAdi.restful_api.dto.ProductResponse;
import ariskaAdi.restful_api.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.findPage(page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

Note the route moved to `/api/v1/products` — versioning decided now, not retrofitted later.

---

## Phase 4 — Validation & Error Handling

**Add dependency to `pom.xml`:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**`ProductRequest.java` — add constraints:**
```java
package ariskaAdi.restful_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
    private BigDecimal price;

    @NotNull
    @Min(value = 0, message = "stock cannot be negative")
    private Integer stock;
}
```

**`src/main/java/ariskaAdi/restful_api/exception/ResourceNotFoundException.java`**
```java
package ariskaAdi.restful_api.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

Replace the `NoSuchElementException` throws in `ProductService` with `new ResourceNotFoundException(...)`.

**`src/main/java/ariskaAdi/restful_api/exception/ErrorResponse.java`**
```java
package ariskaAdi.restful_api.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> fieldErrors;
}
```

**`src/main/java/ariskaAdi/restful_api/exception/GlobalExceptionHandler.java`**
```java
package ariskaAdi.restful_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(req.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(req.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(body);
    }
}
```

**Controller — add `@Valid`:**
```java
@PostMapping
public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) { ... }

@PutMapping("/{id}")
public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) { ... }
```

---

## Phase 5 — MyBatis Deep Dive (XML mappers, relationships, dynamic SQL)

**SQL — add categories + link products to them:**
```sql
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

ALTER TABLE products ADD COLUMN category_id INT REFERENCES categories(id);

INSERT INTO categories (name) VALUES ('Peripherals'), ('Displays');
UPDATE products SET category_id = 1 WHERE name IN ('Keyboard', 'Mouse');
UPDATE products SET category_id = 2 WHERE name = 'Monitor';
```

**`src/main/java/ariskaAdi/restful_api/model/Category.java`**
```java
package ariskaAdi.restful_api.model;

import lombok.Data;
import java.util.List;

@Data
public class Category {
    private Long id;
    private String name;
    private List<Product> products;
}
```

**Switch `ProductMapper` to an XML-backed interface (remove the `@Select`/`@Insert` annotations, keep the method signatures):**

**`src/main/java/ariskaAdi/restful_api/mapper/ProductMapper.java`**
```java
package ariskaAdi.restful_api.mapper;

import ariskaAdi.restful_api.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductMapper {
    List<Product> search(@Param("name") String name,
                          @Param("categoryId") Long categoryId,
                          @Param("sortBy") String sortBy,
                          @Param("size") int size,
                          @Param("offset") int offset);

    long count(@Param("name") String name, @Param("categoryId") Long categoryId);

    Optional<Product> findById(Long id);
    void insert(Product product);
    int update(Product product);
    int delete(Long id);
}
```

**`src/main/resources/mapper/ProductMapper.xml`**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="ariskaAdi.restful_api.mapper.ProductMapper">

    <resultMap id="ProductResultMap" type="ariskaAdi.restful_api.model.Product">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="price" column="price"/>
        <result property="stock" column="stock"/>
        <result property="createdAt" column="created_at"/>
    </resultMap>

    <!-- dynamic SQL: filters only apply if the param is present -->
    <select id="search" resultMap="ProductResultMap">
        SELECT * FROM products
        <where>
            <if test="name != null and name != ''">
                AND name ILIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="categoryId != null">
                AND category_id = #{categoryId}
            </if>
        </where>
        ORDER BY
        <choose>
            <when test="sortBy == 'price'">price</when>
            <when test="sortBy == 'name'">name</when>
            <otherwise>id</otherwise>
        </choose>
        LIMIT #{size} OFFSET #{offset}
    </select>

    <select id="count" resultType="long">
        SELECT COUNT(*) FROM products
        <where>
            <if test="name != null and name != ''">
                AND name ILIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="categoryId != null">
                AND category_id = #{categoryId}
            </if>
        </where>
    </select>

    <select id="findById" resultMap="ProductResultMap">
        SELECT * FROM products WHERE id = #{id}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO products (name, price, stock) VALUES (#{name}, #{price}, #{stock})
    </insert>

    <update id="update">
        UPDATE products SET name=#{name}, price=#{price}, stock=#{stock} WHERE id=#{id}
    </update>

    <delete id="delete">
        DELETE FROM products WHERE id = #{id}
    </delete>
</mapper>
```

Notice `sortBy` is matched against a fixed `<choose>` list, not interpolated directly — that's the SQL-injection-safe way to let users control ORDER BY.

**`src/main/resources/mapper/CategoryMapper.xml` — one-to-many example:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="ariskaAdi.restful_api.mapper.CategoryMapper">

    <resultMap id="CategoryWithProducts" type="ariskaAdi.restful_api.model.Category">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <collection property="products" ofType="ariskaAdi.restful_api.model.Product">
            <id property="id" column="p_id"/>
            <result property="name" column="p_name"/>
            <result property="price" column="p_price"/>
            <result property="stock" column="p_stock"/>
        </collection>
    </resultMap>

    <select id="findWithProducts" resultMap="CategoryWithProducts">
        SELECT c.id, c.name,
               p.id AS p_id, p.name AS p_name, p.price AS p_price, p.stock AS p_stock
        FROM categories c
        LEFT JOIN products p ON p.category_id = c.id
        WHERE c.id = #{id}
    </select>
</mapper>
```

**`src/main/java/ariskaAdi/restful_api/mapper/CategoryMapper.java`**
```java
package ariskaAdi.restful_api.mapper;

import ariskaAdi.restful_api.model.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface CategoryMapper {
    Optional<Category> findWithProducts(Long id);
}
```

Update `ProductService.findPage` to call `productMapper.search(...)` + `productMapper.count(...)` and return both the page and a total in the response (e.g. wrap in a small `PageResponse<T>` DTO with `content`, `page`, `size`, `totalElements`).

---

## Phase 6 — Flyway Migrations

**`pom.xml`**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**`src/main/resources/db/migration/V1__init_schema.sql`**
```sql
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    category_id INT REFERENCES categories(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

**`src/main/resources/db/migration/V2__seed_data.sql`**
```sql
INSERT INTO categories (name) VALUES ('Peripherals'), ('Displays');

INSERT INTO products (name, price, stock, category_id) VALUES
  ('Keyboard', 49.99, 100, 1),
  ('Mouse', 19.99, 200, 1),
  ('Monitor', 199.99, 50, 2);
```

Drop your manually-created tables first (`DROP TABLE products, categories CASCADE;`) and let Flyway recreate them on next boot — from now on, all schema changes are new `V{n}__description.sql` files, never manual `ALTER TABLE`.

---

## Phase 7 — Testing

**`pom.xml` — add Testcontainers:**
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

**`src/test/java/ariskaAdi/restful_api/service/ProductServiceTest.java` — unit test, mapper mocked:**
```java
package ariskaAdi.restful_api.service;

import ariskaAdi.restful_api.exception.ResourceNotFoundException;
import ariskaAdi.restful_api.mapper.ProductMapper;
import ariskaAdi.restful_api.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void findById_throwsWhenMissing() {
        when(productMapper.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

**`src/test/java/ariskaAdi/restful_api/mapper/ProductMapperIT.java` — integration test against real Postgres:**
```java
package ariskaAdi.restful_api.mapper;

import ariskaAdi.restful_api.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropyRegistrar; // placeholder, see note below
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class ProductMapperIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @org.springframework.test.context.DynamicPropertySource
    static void overrideProps(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductMapper productMapper;

    @Test
    void insertAndFind() {
        Product product = new Product();
        product.setName("Webcam");
        product.setPrice(new BigDecimal("59.90"));
        product.setStock(10);

        productMapper.insert(product);

        assertThat(productMapper.findById(product.getId())).isPresent();
    }
}
```
(Remove the bogus `DynamicPropyRegistrar` import line — leftover placeholder, the real one is `@DynamicPropertySource` used above.)

**`src/test/java/ariskaAdi/restful_api/controller/ProductControllerTest.java` — MockMvc:**
```java
package ariskaAdi.restful_api.controller;

import ariskaAdi.restful_api.dto.ProductResponse;
import ariskaAdi.restful_api.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void getAll_returnsOk() throws Exception {
        when(productService.findPage(0, 20)).thenReturn(List.of(new ProductResponse()));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }
}
```

---

## Phase 8 — API Documentation

**`pom.xml`**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

**Annotate the controller:**
```java
@Tag(name = "Products", description = "Product catalog operations")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    @Operation(summary = "List products", description = "Paginated product listing")
    @GetMapping
    public List<ProductResponse> getAll(...) { ... }
}
```
Run the app, open `http://localhost:8080/swagger-ui.html`.

---

## Phase 9 — Security (JWT sketch)

**`pom.xml`**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
```

**`src/main/java/ariskaAdi/restful_api/security/SecurityConfig.java` — skeleton:**
```java
package ariskaAdi.restful_api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated());
        // add your JWT filter here with .addFilterBefore(...)
        return http.build();
    }
}
```
This is intentionally a skeleton — the full JWT filter (token generation, parsing, `OncePerRequestFilter`) is a bigger chunk of code; ask when you get here and we'll write it against your actual login flow.

---

## Phase 10 — Production Readiness (snippets)

**`application-dev.properties`**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/restful_api
logging.level.ariskaAdi.restful_api=DEBUG
```

**`application-prod.properties`**
```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USER}
spring.datasource.password=${DATABASE_PASSWORD}
logging.level.root=WARN
```

**Actuator — `pom.xml`:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**`Dockerfile`**
```dockerfile
FROM eclipse-temurin:17-jre
COPY target/restful-api-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

**`docker-compose.yml`**
```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: restful_api
      POSTGRES_PASSWORD: postgres
    ports: ["5432:5432"]
  app:
    build: .
    depends_on: [db]
    environment:
      DATABASE_URL: jdbc:postgresql://db:5432/restful_api
      DATABASE_USER: postgres
      DATABASE_PASSWORD: postgres
    ports: ["8080:8080"]
```

---

## Phase 11 — Stretch Goals

No dummy code here on purpose — these are optional and very implementation-specific (rate limiting library choice, HATEOAS link builders, Kafka setup, module layout). When you're ready to pick one, ask and we'll write it against the real state of the project at that point.

---

### How to use this file
Copy each block in as you reach that phase in `todos.md`, run the app, hit the endpoint, confirm it behaves as described, *then* move on. If something doesn't compile or doesn't match your actual files (package names, existing methods), stop and ask rather than guessing — this is dummy/reference code, not a diff against your real project.
