# AGENTS.md

## Layout: 4 independent Maven projects, no aggregator

There is no root `pom.xml`. Each directory below is its own build with its own wrapper. Never run Maven from the repo root.

| Service | Port | Role |
|---|---|---|
| `discovery-service` | 8761 | Eureka server (`@EnableEurekaServer`) |
| `gateway-service` | 8080 | Spring Cloud Gateway (WebFlux) + resilience4j circuit breakers |
| `product-service` | 8081 | JPA product CRUD on H2 |
| `order-service` | 8082 | JPA orders + OpenFeign calls to product-service |

**Version skew:** `order-service` is Spring Boot `4.1.1` / spring-cloud `2025.1.2` / wrapper Maven `3.9.16`. The other three are `4.0.8` / `2025.1.1` / `3.9.15`. When copying a pom or config between services, sync all three or you will build against the wrong API surface.

## Commands

- `mvn` is **not** installed. Use the per-service wrapper: `cd <service> && ./mvnw <goal>`.
- `~/.m2` is fully populated, so `./mvnw -o <goal>` (offline) works and is much faster. Verified: `./mvnw -o test` passes in `product-service`.
- Verify a change: `cd <service> && ./mvnw -o test`. There is no aggregate build, so each touched service must be built separately.
- **No lint, format, checkstyle, or spotless plugin exists in any pom.** There is no lint step — don't add one unless asked.
- Testing is near-zero right now: each service has exactly one `@SpringBootTest contextLoads()` smoke test. `mvn test` proves "the context starts", nothing more. For real behavior verification use the `test.http` files (IDE HTTP client), not tests.
- No CI config (`.github/`, etc.) in the repo.

## Running the stack

- Start `discovery-service` first; the other three register against `http://localhost:8761/eureka/`.
- Eureka registration failures are only **log noise** — contexts still start and tests still pass with discovery down. A green test run does not mean service wiring works.
- `.vscode/launch.json` (untracked, gitignored) has per-service launch configs; it references a `${workspaceFolder}/.env` that does not exist.

## Data

- `product-service`: H2 in-mem `productdb`, `ddl-auto: create-drop`, seeded from `src/main/resources/data.sql` (5 products, `prod-001`..`prod-005`). `spring.jpa.defer-datasource-initialization: true` is what lets `data.sql` run after Hibernate creates the schema — keep it if you touch the JPA config.
- `order-service`: H2 `orderdb`, `create-drop`, **no** seed data.
- All data is lost on restart. H2 consoles are enabled at `/h2-console` on both JPA services.

## Cross-service contract

- Gateway routes live in `gateway-service/src/main/resources/application.yaml`: `/api/v1/products/**` → `lb://product-service`, `/api/v1/orders/**` → `lb://order-service`. Each has a resilience4j `CircuitBreaker` filter with `fallbackUri: forward:/fallback/{product,order}`, served by `FallbackController`.
- `order-service` reaches product-service **directly via Feign** (`ProductClient`, Eureka name `product-service`), not through the gateway.
- **Every successful response from product-service and order-service is wrapped in `ApiResponse<T>` (`{message, data}`)**, except `DELETE`, which stays `204 No Content` (a 204 cannot carry a body). Errors are *not* wrapped: both services have an identical `@RestControllerAdvice` (`GlobalExceptionHandler`) returning `ErrorResponse(timestamp, status, error, message)`.
- **Because of that envelope, `ProductClient` returns `ApiResponse<ProductDto>`, not `ProductDto`.** `OrderService` must call `.data()` to reach the product. If you ever unwrap at the client instead, every Feign method and `ProductClientFallback` change together — they are the two places that can silently break.
- `ApiResponse` and `ProductDto` are both **duplicated by design** in the two services as independent records. Changing either contract in product-service means hand-editing `order-service/src/main/java/com/ecommerce/orderservice/dto/{ApiResponse,ProductDto}.java` too — nothing enforces the match.
- `ProductClient` paths have no leading slash and no service prefix; keep them in sync with `ProductController` (`api/v1/products/...`).

## Known gaps — verify before relying on them

- `ProductClient` declares `fallback = ProductClientFallback.class`, but order-service has **no circuit-breaker dependency** and does not set `spring.cloud.openfeign.circuitbreaker.enabled`. Spring Cloud OpenFeign only honors `fallback` when a `CircuitBreakerFactory` bean is present, so `ProductClientFallback` is currently dead code and a product-service outage propagates as a raw Feign exception.
- `GlobalExceptionHandler` is **not** identical across services, on purpose. Shared cases (keep in sync): `EntityNotFoundException` → 404, `MethodArgumentNotValidException` → 400 with a field→message map in the `error` slot, `InsufficientStockException` → 409, catch-all `Exception` → 500. product-service adds `IllegalArgumentException` → 400. order-service adds `OrderNotCancelableException` → 409 and `FeignException` → 503.
- Errors are **not** wrapped in `ApiResponse`, and the catch-all never leaks `ex.getMessage()` — it logs the stack and returns a fixed string, because a raw message can carry SQL, class names, or internal URLs. Adding a handler that echoes an exception message is the thing to watch for in review.
- Do not map exceptions by catching a broad type to get a status: `IllegalStateException` → 409 was wrong and was removed in favour of typed domain exceptions. Same reasoning applies to `IllegalArgumentException` → 400 in product-service; it is there because `ProductController` validates request params inline and throws it (`ProductController.java:64,75`). That validation arguably belongs in the service layer.
- The 503 path is verified end to end: with product-service registered but down, Feign throws `feign.RetryableException` (a `FeignException` subclass) and order-service returns 503. If Eureka is down, order-service keeps serving from its cached registry and the same 503 results.
- `order-service/.../dto/OrderResponse.java` was unused and is now the API response DTO: `OrderService` returns it and `OrderController` only delegates. The mapping is hand-written in `mapper/OrderMapper` (a plain `@Component`), not MapStruct — see the note below.
- **Do not reintroduce MapStruct in this repo (Lombok 1.18.46 + JDK 25).** A `@Mapper` interface's generated `*MapperImpl.java` correctly declares `implements XMapper`, but the compiled `.class` *intermittently* loses the `implements` clause. Spring still registers the bean definition (visible at TRACE), so the failure is `No qualifying bean of type 'ProductMapper'` — don't chase component-scan. The cause is **Lombok patching the classfile**, not MapStruct: three configs were each run 3-6 times and all still produced a corrupt class at least once — MapStruct 1.6.3 (2/3 broken), 1.7.0.Beta2 (1/5), 1.7.0.Beta2 + `lombok-mapstruct-binding:0.2.0` (1/6). A `clean compile` "fixing" it is a coin flip, not a repair; a green run proves nothing. (An old report with this exact failure sits in `product-service/target/surefire-reports/` — the bug predates any of this.) Both services therefore hand-write their mappers as plain `@Component` classes. Lombok 1.18.46 is pinned by the Spring Boot parent — upgrading Lombok or moving off JDK 25 is the only real fix, and neither was tested here.
- `order-service/test.http` covers **only** `POST /api/v1/orders` (path is correct end to end). It never exercises GET/PATCH/DELETE on orders, so it is not a substitute for `mvn test`. `product-service/test.http` is a list of GETs only, with no POST/PUT/DELETE.
- Feign debug logging is enabled via `logging.level.com.ecommerce.orderservice: DEBUG` in order-service's yaml. It is package-root wide, not scoped to `client`, so it also turns DEBUG on for `OrderService`, `OrderController` and the mapper.

## Code conventions

- Java 25. Records for DTOs; Lombok `@Data` / `@Builder` on entities; constructor injection via `@RequiredArgsConstructor`.
- Entity ↔ DTO conversion belongs in the **service** layer, not the controller: services return DTOs, controllers only delegate. Both services hand-write their mapper as a plain `@Component` (order-service in `mapper/OrderMapper`, product-service in `mapper/ProductMapper`) — see the MapStruct note above.
- Comments and inline config comments are written in Spanish. Match that when editing existing files.
- Timestamps come from `@PrePersist` / `@PreUpdate`: order-service and `GlobalExceptionHandler` use UTC, product-service entities use `ZoneId.systemDefault()`. Stay consistent within a service.
