# E-commerce — Microservicios con Spring Cloud

API REST de comercio electrónico dividida en cuatro microservicios independientes, con descubrimiento de servicios (Eureka), API Gateway con circuit breakers (Resilience4j) y comunicación síncrona entre servicios vía OpenFeign.


## Servicios

| Servicio | Puerto | Rol | Persistencia |
|---|---|---|---|
| `discovery-service` | 8761 | Servidor Eureka (`@EnableEurekaServer`) | — |
| `gateway-service` | 8080 | API Gateway (WebFlux) + circuit breakers + fallbacks | — |
| `product-service` | 8081 | CRUD de productos (JPA) | H2 en memoria `productdb` |
| `order-service` | 8082 | Gestión de pedidos; consulta stock vía OpenFeign | H2 en memoria `orderdb` |

Cada directorio es un proyecto Maven propio **sin `pom.xml` raíz ni build agregado**: se compila y ejecuta por separado.

## Stack tecnológico

- **Java 25**, records para DTOs, Lombok en los servicios con dominio (`@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`)
- **Spring Boot 4.0.8** y **Spring Cloud 2025.1.1** (`order-service`: Boot **4.1.1** / Cloud **2025.1.2**)
- Spring Cloud Gateway (WebFlux), Netflix Eureka, OpenFeign + Apache HttpClient 5
- Resilience4j (circuit breakers en el gateway)
- Spring Data JPA + Hibernate + H2 (memoria, `ddl-auto: create-drop`)
- Bean Validation (`@Valid`, `@NotBlank`, `@Positive`…)
- Maven Wrapper 3.3.4 en cada servicio (Maven no necesita estar instalado)

## Estructura del repositorio

```
e-commerce/
├── discovery-service/     # Eureka Server
├── gateway-service/       # Gateway + FallbackController
├── product-service/       # CRUD de productos (+ data.sql de ejemplo)
│   └── test.http
├── order-service/         # Pedidos + ProductClient (Feign)
│   └── test.http
└── AGENTS.md              # Guía de arquitectura y convenciones
```

## Requisitos

- JDK 25

## Puesta en marcha

**Orden de arranque** — primero el discovery, luego el resto (se registran en Eureka):

```bash
cd discovery-service && ./mvnw spring-boot:run
cd gateway-service   && ./mvnw spring-boot:run
cd product-service   && ./mvnw spring-boot:run
cd order-service     && ./mvnw spring-boot:run
```

> Los fallos de registro en Eureka mientras arrancan son solo ruido de log: los contextos arrancan igualmente.

Verificación:

- Eureka UI: <http://localhost:8761>
- Gateway: <http://localhost:8080/api/v1/products>
- Consolas H2 (`sa`, sin password): <http://localhost:8081/h2-console> y <http://localhost:8082/h2-console>
  - JDBC URL: `jdbc:h2:mem:productdb` / `jdbc:h2:mem:orderdb`
- Actuator (`health`, `info`) expuesto en todos los servicios


## API

### Contrato de respuestas

Todos los éxitos de `product-service` y `order-service` vienen envueltos en un sobre `ApiResponse<T>`:

```json
{ "message": "Products retrieved", "data": [ ... ] }
```

- `DELETE` responde **204 No Content** (sin body).
- Los errores **no** se envuelven: usan `ErrorResponse(timestamp, status, error, message)`.

### Productos — `product-service` (vía gateway: `http://localhost:8080`)

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/api/v1/products` | Lista todos los productos | 200 |
| GET | `/api/v1/products/{id}` | Producto por id | 200 / 404 |
| GET | `/api/v1/products/category/{category}` | Filtra por categoría | 200 |
| GET | `/api/v1/products/health` | Health check del servicio | 200 |
| POST | `/api/v1/products` | Crea un producto (`ProductDto` validado) | 201 |
| PUT | `/api/v1/products/{id}` | Actualiza un producto | 200 / 404 |
| DELETE | `/api/v1/products/{id}` | Elimina un producto | 204 / 404 |
| PATCH | `/api/v1/products/{id}/stock` | Fija el stock — body `{"quantity": n}` | 200 |
| PATCH | `/api/v1/products/{id}/stock/decrement` | Decrementa stock — body `{"amount": n}` | 200 / 409 |

### Pedidos — `order-service` (vía gateway: `http://localhost:8080`)

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/api/v1/orders` | Lista todos los pedidos | 200 |
| GET | `/api/v1/orders/{id}` | Pedido por id | 200 / 404 |
| GET | `/api/v1/orders/health` | Health check del servicio | 200 |
| POST | `/api/v1/orders` | Crea pedido (`CreateOrderRequest`) — valida stock contra product-service | 201 |
| PATCH | `/api/v1/orders/{id}/status` | Cambia estado — body `{"status": "CONFIRMED"}` | 200 |
| DELETE | `/api/v1/orders/{id}` | Cancela el pedido (409 si está `DELIVERED`) | 204 / 409 |

Estados de pedido: `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`.

### Ejemplos de consumo

Todos los ejemplos van contra el gateway (`http://localhost:8080`). Las respuestas usan datos del seed de `data.sql`.

**Listar productos**

```bash
curl http://localhost:8080/api/v1/products
```

```json
{
  "message": "Products retrieved",
  "data": [
    {
      "id": "prod-001",
      "name": "Laptop Dell XPS 13",
      "description": "Ultrabook con Intel i7, 16GB RAM, 512GB SSD",
      "price": 1299.99,
      "stockQuantity": 15,
      "category": "Electronics",
      "imageUrl": "https://example.com/laptop.jpg",
      "createdAt": "2026-10-08T12:00:00",
      "updatedAt": "2026-10-08T12:00:00"
    },
    {
      "id": "prod-002",
      "name": "iPhone 15 Pro",
      "description": "Smartphone 256GB, Titanio Natural",
      "price": 1199.00,
      "stockQuantity": 8,
      "category": "Electronics",
      "imageUrl": "https://example.com/iphone.jpg",
      "createdAt": "2026-10-08T12:00:00",
      "updatedAt": "2026-10-08T12:00:00"
    }
  ]
}
```

**Crear producto** — `201 Created` (el `id` y los timestamps los genera el servidor):

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Auriculares Sony WH-1000XM5",
    "description": "Cancelación de ruido activa",
    "price": 349.99,
    "stockQuantity": 20,
    "category": "Electronics",
    "imageUrl": "https://example.com/auriculares.jpg"
  }'
```

```json
{
  "message": "Product created",
  "data": {
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "name": "Auriculares Sony WH-1000XM5",
    "description": "Cancelación de ruido activa",
    "price": 349.99,
    "stockQuantity": 20,
    "category": "Electronics",
    "imageUrl": "https://example.com/auriculares.jpg",
    "createdAt": "2026-10-08T12:30:00",
    "updatedAt": "2026-10-08T12:30:00"
  }
}
```

**Crear pedido** — `201 Created`. Internamente `order-service` consulta cada producto y decrementa su stock vía Feign contra `product-service`; por eso el pedido nace directamente en `CONFIRMED` y los precios son los del momento de la compra:

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-001",
    "items": [
      {"productId": "prod-001", "quantity": 2},
      {"productId": "prod-003", "quantity": 1}
    ]
  }'
```

```json
{
  "message": "Order created",
  "data": {
    "id": "d4c7a1e2-9b3f-4e8a-8c5d-1f2a6b0e9d7c",
    "customerId": "cust-001",
    "items": [
      {
        "id": "e1f2a3b4-c5d6-4789-9abc-def012345678",
        "productId": "prod-001",
        "productName": "Laptop Dell XPS 13",
        "quantity": 2,
        "price": 1299.99
      },
      {
        "id": "f2a3b4c5-d6e7-4890-abcd-ef0123456789",
        "productId": "prod-003",
        "productName": "Nike Air Max 90",
        "quantity": 1,
        "price": 129.99
      }
    ],
    "totalAmount": 2729.97,
    "status": "CONFIRMED",
    "createdAt": "2026-10-08T12:35:00",
    "updatedAt": "2026-10-08T12:35:00"
  }
}
```

**Actualizar estado del pedido**

```bash
curl -X PATCH http://localhost:8080/api/v1/orders/d4c7a1e2-9b3f-4e8a-8c5d-1f2a6b0e9d7c/status \
  -H "Content-Type: application/json" \
  -d '{"status": "SHIPPED"}'
```

```json
{
  "message": "Order status updated",
  "data": {
    "id": "d4c7a1e2-9b3f-4e8a-8c5d-1f2a6b0e9d7c",
    "customerId": "cust-001",
    "items": [
      {
        "id": "e1f2a3b4-c5d6-4789-9abc-def012345678",
        "productId": "prod-001",
        "productName": "Laptop Dell XPS 13",
        "quantity": 2,
        "price": 1299.99
      },
      {
        "id": "f2a3b4c5-d6e7-4890-abcd-ef0123456789",
        "productId": "prod-003",
        "productName": "Nike Air Max 90",
        "quantity": 1,
        "price": 129.99
      }
    ],
    "totalAmount": 2729.97,
    "status": "SHIPPED",
    "createdAt": "2026-10-08T12:35:00",
    "updatedAt": "2026-10-08T12:40:00"
  }
}
```

**Cancelar pedido** — sin body, `204 No Content`:

```bash
curl -X DELETE http://localhost:8080/api/v1/orders/d4c7a1e2-9b3f-4e8a-8c5d-1f2a6b0e9d7c
```

**Error: producto inexistente** — `404 Not Found` (los errores no llevan el sobre `ApiResponse`):

```bash
curl http://localhost:8080/api/v1/products/prod-999
```

```json
{
  "timestamp": "2026-10-08T12:45:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found: prod-999"
}
```

**Error: stock insuficiente** — `409 Conflict` (pedido de 100 unidades de un producto con stock 15):

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-002",
    "items": [{"productId": "prod-001", "quantity": 100}]
  }'
```

```json
{
  "timestamp": "2026-10-08T12:50:00",
  "status": 409,
  "error": "Conflict",
  "message": "Insufficient stock for product prod-001. Available: 15, Requested: 100"
}
```

### Errores

| Excepción | Status |
|---|---|
| `MethodArgumentNotValidException` | 400 (en `product-service`: mapa campo→mensaje) |
| `IllegalArgumentException` | 400 (`product-service`) |
| `EntityNotFoundException` | 404 |
| `InsufficientStockException` | 409 |
| `OrderNotCancelableException` | 409 (`order-service`) |
| `FeignException` (product-service caído) | 503 (`order-service`) |
| Cualquier otra | 500 (mensaje fijo, sin filtrar el detalle interno) |

### Gateway y circuit breakers

Rutas definidas en `gateway-service/src/main/resources/application.yaml`:

| Ruta | Destino | Circuit breaker |
|---|---|---|
| `/api/v1/products/**` | `lb://product-service` | `productCircuitBreaker` → `forward:/fallback/product` |
| `/api/v1/orders/**` | `lb://order-service` | `orderCircuitBreaker` → `forward:/fallback/order` |

Resilience4j (por instancia): ventana deslizante de 10 llamadas, umbral de fallo 50 %, 30 s en estado abierto, 3 llamadas permitidas en semiabierto.

Cuando el breaker se abre, `FallbackController` responde:

```json
{ "error": "Service Unavailable", "message": "Product Service is temporarily unavailable. Please try again later.", "timestamp": "..." }
```

`order-service` además llama a `product-service` **directamente vía Feign** (nombre Eureka `product-service`, no pasa por el gateway) para leer el producto y decrementar stock al crear un pedido (timeouts de 5 s).