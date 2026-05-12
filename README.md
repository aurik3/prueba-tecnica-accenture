# Fredy Alejandro Gonzalez Caro - Prueba Tecnica Desarrollador Backend (Spring Boot WebFlux)

API reactiva para administrar franquicias, sucursales y productos con stock.

## Ejecución local

### Docker Compose

```bash
docker compose up -d --build
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Pruebas

Unit tests:

```bash
mvn -q clean test
```

Integration tests:

- Usan Testcontainers (requiere Docker). Si Docker no está disponible, se omiten automáticamente.

## Arquitectura

La solución está organizada siguiendo Clean Architecture en 4 capas principales, con dependencias dirigidas hacia el núcleo:

- Presentation → Application → Domain
- Infrastructure → (implementa puertos de Application, usando modelos de Domain)

### 1) Domain (núcleo)

Contiene el modelo de negocio puro y validaciones del dominio, sin depender de Spring ni de la base de datos.

- Modelos:
  - [Franchise.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/domain/model/Franchise.java)
  - [Branch.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/domain/model/Branch.java)
  - [Product.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/domain/model/Product.java)
- Reglas:
  - Normalización/validación de nombres y stock en el propio dominio (ej. `stock >= 0`).
  - Las operaciones de cambio devuelven nuevos objetos inmutables (records) en lugar de mutar estado.

### 2) Application (casos de uso)

Orquesta los casos de uso y aplica reglas de aplicación (existencia, unicidad por contexto, etc.), utilizando puertos para acceder a la persistencia.

- Casos de uso “Command” (escritura):
  - Crear/renombrar franquicia: [FranchiseCommandService.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/service/FranchiseCommandService.java)
  - Crear/renombrar sucursal: [BranchCommandService.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/service/BranchCommandService.java)
  - Crear/eliminar/renombrar producto y actualizar stock: [ProductCommandService.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/service/ProductCommandService.java)
- Casos de uso “Query” (lectura/consultas):
  - Consulta de franquicia/sucursales/productos y reporte de máximo stock: [FranchiseQueryService.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/service/FranchiseQueryService.java)
- Puertos de salida (interfaces) para persistencia:
  - [FranchiseRepositoryPort.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/port/out/FranchiseRepositoryPort.java)
  - [BranchRepositoryPort.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/port/out/BranchRepositoryPort.java)
  - [ProductRepositoryPort.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/port/out/ProductRepositoryPort.java)

### 3) Infrastructure (persistencia MongoDB reactiva)

Implementa los puertos de Application mediante adaptadores que usan Spring Data Reactive MongoDB. Esta capa contiene:

- Documentos/colecciones Mongo (DTOs de persistencia).
- Repositorios Spring Data (ReactiveCrudRepository).
- Adaptadores que traducen Document ↔ Domain.

Ejemplos:
- Adaptadores:
  - [FranchiseMongoAdapter.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/infrastructure/mongo/adapter/FranchiseMongoAdapter.java)
  - [BranchMongoAdapter.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/infrastructure/mongo/adapter/BranchMongoAdapter.java)
  - [ProductMongoAdapter.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/infrastructure/mongo/adapter/ProductMongoAdapter.java)

Modelo de datos: colecciones separadas (normalizado) para evitar documentos grandes y facilitar operaciones por contexto:

- `franchises` (`id`, `name`)
- `branches` (`id`, `franchiseId`, `name`)
- `products` (`id`, `branchId`, `name`, `stock`)

### 4) Presentation (API WebFlux + DTOs)

Exposición de endpoints reactivos con WebFlux. Esta capa:

- Define rutas y contratos (DTOs request/response).
- Valida entrada (`@Valid`).
- Convierte IDs (string → UUID) y delega la lógica a Application.

Controladores:
- Root/health informativo: [RootController.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/presentation/controller/RootController.java)
- Franquicias/sucursales: [FranchiseController.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/presentation/controller/FranchiseController.java), [BranchController.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/presentation/controller/BranchController.java)
- Productos (bajo sucursal): [ProductController.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/presentation/controller/ProductController.java)

Manejo de errores:
- Respuestas consistentes para `400/404/409/500` desde: [GlobalExceptionHandler.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/presentation/error/GlobalExceptionHandler.java)

### Flujo de una operación (ejemplo)

Actualizar stock de un producto:

1. Request llega a `PATCH /api/branches/{branchId}/products/{productId}/stock` en Presentation.
2. El controlador valida payload y convierte IDs a UUID.
3. Application ejecuta el caso de uso (`ProductCommandService`) validando que el producto pertenezca a la sucursal.
4. Infrastructure consulta/actualiza en Mongo (adapter/repository) y retorna el `Product` de Domain actualizado.
5. Presentation serializa la respuesta (`ProductResponse`).

## Endpoints por feature

### Franquicias

- `POST /api/franchises`
- `GET /api/franchises`
- `GET /api/franchises/{franchiseId}`
- `PATCH /api/franchises/{franchiseId}`

### Sucursales

- `POST /api/franchises/{franchiseId}/branches`
- `GET /api/franchises/{franchiseId}/branches`
- `GET /api/branches`
- `GET /api/branches/{branchId}`
- `PATCH /api/branches/{branchId}`

### Productos (siempre bajo sucursal)

- `POST /api/branches/{branchId}/products`
- `GET /api/branches/{branchId}/products`
- `GET /api/branches/{branchId}/products/{productId}`
- `PATCH /api/branches/{branchId}/products/{productId}`
- `PATCH /api/branches/{branchId}/products/{productId}/stock`
- `DELETE /api/branches/{branchId}/products/{productId}`

### Reportes

- `GET /api/franchises/{franchiseId}/branches/max-stock-products`
