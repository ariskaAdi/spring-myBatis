# RESTful API Learning Roadmap — Spring Boot + MyBatis + PostgreSQL

Current state of this project (checked 2026-07-25):
- Spring Boot `4.0.7`, Java `17`
- Dependencies already in `pom.xml`: `spring-boot-starter-webmvc`, `mybatis-spring-boot-starter` (4.0.1), `postgresql` driver, `lombok`, plus test starters for webmvc and mybatis
- Nothing configured yet: `application.properties` only has `spring.application.name`, no datasource, no packages beyond the main `RestfulApiApplication` class

Suggested domain to build throughout: a simple **Product Catalog API** (`products`, `categories`). Small enough to not distract from the concepts, rich enough to need relationships/filtering later.

Work top to bottom. Each phase builds on the last — don't skip to Security before you have working CRUD.

---

## Phase 0 — Environment Check
- [ ] Confirm build works: `./mvnw clean install`
- [ ] Install PostgreSQL locally, or run via Docker (recommended so it's disposable/reproducible):
      `docker run --name pg-restful -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=restful_api -p 5432:5432 -d postgres:16`
- [ ] Install a DB client to inspect data (DBeaver, or plain `psql`)
- [ ] Install a REST client for testing endpoints (Postman, Insomnia, or `curl`/`.http` files in VS Code)

## Phase 1 — Hello Database (first working endpoint)
- [ ] Add datasource config to `application.properties` (url, username, password, `spring.datasource.driver-class-name`)
- [ ] Create the `products` table manually via SQL (id, name, price, stock, created_at)
- [ ] Create package structure: `controller`, `service`, `mapper`, `model` (or `entity`), `dto`
- [ ] Create a `Product` model class (use Lombok `@Data`)
- [ ] Create a `ProductMapper` interface with `@Mapper` and a simple `@Select("SELECT * FROM products")` method
- [ ] Create a `ProductController` with `GET /api/products` that calls the mapper directly (skip the service layer just this once, to see the full request→DB round trip)
- [ ] Run the app and confirm the endpoint returns data

## Phase 2 — Proper Layered Architecture
- [ ] Introduce a `ProductService` between controller and mapper — controller should never call the mapper directly from here on
- [ ] Introduce DTOs (`ProductRequest`, `ProductResponse`) — never expose your DB entity directly over the wire
- [ ] Add mapping between entity ↔ DTO (by hand first; consider MapStruct later once you feel the boilerplate pain)
- [ ] Use constructor injection everywhere (not `@Autowired` field injection)

## Phase 3 — Full CRUD + REST Conventions
- [ ] Implement all endpoints: `GET /api/products`, `GET /api/products/{id}`, `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}`
- [ ] Return correct status codes: `201 Created` with a `Location` header on create, `204 No Content` on delete, `404 Not Found` when missing, `200 OK` otherwise
- [ ] Use `ResponseEntity<T>` deliberately instead of returning raw objects
- [ ] Add pagination via query params (`page`, `size`) and return total count/metadata alongside the list
- [ ] Decide and document your naming/versioning convention now (e.g. `/api/v1/products`) — cheap to do early, painful to retrofit

## Phase 4 — Validation & Error Handling
- [ ] Add `spring-boot-starter-validation` dependency
- [ ] Annotate request DTOs (`@NotBlank`, `@Min`, `@Size`, etc.) and `@Valid` them in controller method params
- [ ] Create custom exceptions (`ResourceNotFoundException`, `DuplicateResourceException`)
- [ ] Add a `@RestControllerAdvice` global exception handler
- [ ] Standardize your error response shape (timestamp, status, error, message, path, field-level validation errors) — pick one shape and reuse it everywhere

## Phase 5 — MyBatis Deep Dive
- [ ] Convert from annotation-based mapper methods to XML mapper files (`ProductMapper.xml`) — learn `resultMap`, parameter binding, and why XML is preferred for anything beyond trivial queries
- [ ] Add a `categories` table and a one-to-many relationship (category → products); model it with a `resultMap` collection
- [ ] Implement dynamic SQL filtering with `<if>`, `<where>`, `<foreach>` (e.g. filter products by name/category/price range)
- [ ] Implement sorting via a query param — **whitelist allowed columns** server-side, never interpolate the raw param into SQL
- [ ] Implement real pagination in SQL (`LIMIT`/`OFFSET`) plus a separate `COUNT(*)` query for total records

## Phase 6 — Database Migrations & Schema Management
- [ ] Add Flyway (`flyway-core` + `flyway-database-postgresql`)
- [ ] Move your schema into versioned migration scripts (`V1__init_schema.sql`, `V2__add_categories.sql`, ...)
- [ ] Add a seed-data migration for local dev
- [ ] Stop hand-editing the DB — from now on, every schema change goes through a migration

## Phase 7 — Testing
- [ ] Unit test `ProductService` with Mockito (mock the mapper, test business logic in isolation)
- [ ] Test mappers with `mybatis-spring-boot-starter-test`
- [ ] Add Testcontainers (`testcontainers` + `testcontainers-postgresql`) so integration tests run against a real, disposable Postgres instead of assumptions or H2
- [ ] Integration-test controllers with `@SpringBootTest` + `MockMvc` (full request → DB → response round trip)
- [ ] Add a coverage tool (JaCoCo) if you want visibility into gaps

## Phase 8 — API Documentation
- [ ] Add `springdoc-openapi-starter-webmvc-ui`
- [ ] Annotate controllers/DTOs enough to produce a useful Swagger UI at `/swagger-ui.html`
- [ ] Keep example request/response bodies in the annotations up to date as the API evolves

## Phase 9 — Security
- [ ] Add `spring-boot-starter-security`
- [ ] Implement JWT-based authentication (login endpoint issuing a token)
- [ ] Add role-based authorization with `@PreAuthorize` on sensitive endpoints
- [ ] Hash passwords with BCrypt — never store plaintext
- [ ] Add CORS configuration deliberately (don't leave it wide open by accident)

## Phase 10 — Production Readiness
- [ ] Split config by profile: `application-dev.properties`, `application-prod.properties`, activate via `spring.profiles.active`
- [ ] Add Spring Boot Actuator (`/actuator/health`, `/actuator/metrics`)
- [ ] Switch to structured logging (SLF4J/Logback), add a request/correlation ID per request
- [ ] Add `@Transactional` where multi-step writes need atomicity; understand what MyBatis does and doesn't auto-manage here
- [ ] Add caching (`@Cacheable`, Caffeine or Redis) for read-heavy endpoints
- [ ] Dockerize the app itself (`Dockerfile`) and write a `docker-compose.yml` that runs app + Postgres together
- [ ] Add a CI pipeline (GitHub Actions): build, run tests, fail on broken build

## Phase 11 — Stretch Goals (optional, pick based on interest)
- [ ] Rate limiting (bucket4j) on public endpoints
- [ ] Optimistic locking (`version` column) for concurrent update safety
- [ ] HATEOAS (`spring-hateoas`) to explore full REST maturity (level 3)
- [ ] Outbox pattern / event publishing (Kafka) if you want to explore event-driven architecture
- [ ] Multi-module Maven layout (api / service / persistence modules) for larger projects

---

### How to use this file
Work sequentially — each phase assumes the previous one's endpoints/tests already work. Check items off as you go (`- [x]`). When a phase introduces a new concept you don't understand yet, stop and ask before moving on — the point is to actually learn MyBatis + REST conventions, not just copy-paste a working app.
