# Fredy Alejandro Gonzalez Caro - Prueba Técnica Backend (Spring Boot WebFlux)

Esta API sirve para administrar franquicias, sus sucursales y los productos que tiene cada sucursal (incluyendo stock). Está hecha con Spring Boot WebFlux (reactiva) y MongoDB.

## Ejecución local

### Docker Compose

```bash
docker compose up -d --build
```

- API: http://localhost:8080
- Documentación (Swagger): http://localhost:8080/swagger-ui.html
- JSON de OpenAPI: http://localhost:8080/v3/api-docs

## Pruebas

Unit tests:

```bash
mvn -q clean test
```

Integration tests:

- Corren con Testcontainers (necesita Docker). Si no hay Docker, se omiten automáticamente.

## Arquitectura

La idea general es simple: separar lo “importante del negocio” de los detalles técnicos (framework y base de datos), para que el proyecto sea fácil de mantener y de probar.

En la práctica, está dividido en 4 capas:

### 1) Domain (el corazón)

Acá viven las reglas del negocio y las entidades principales (Franchise, Branch, Product). Es código “limpio”: no depende de Spring, ni de Mongo, ni de nada externo.

- Modelos:
  - [Franchise.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/domain/model/Franchise.java)
  - [Branch.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/domain/model/Branch.java)
  - [Product.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/domain/model/Product.java)
- Incluye validaciones básicas como nombres válidos y stock no negativo.

### 2) Application (lo que se puede hacer)

Esta capa “orquesta” los casos de uso: crear, renombrar, listar, actualizar stock, etc. También se encarga de validar cosas como “existe / no existe”, “pertenece a esta sucursal”, y de decidir qué error devolver cuando algo no cuadra.

- Casos de uso “Command” (escritura):
  - Crear/renombrar franquicia: [FranchiseCommandService.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/service/FranchiseCommandService.java)
  - Crear/renombrar sucursal: [BranchCommandService.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/service/BranchCommandService.java)
  - Crear/eliminar/renombrar producto y actualizar stock: [ProductCommandService.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/service/ProductCommandService.java)
- Casos de uso “Query” (lectura/consultas):
  - Consulta de franquicia/sucursales/productos y reporte de máximo stock: [FranchiseQueryService.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/service/FranchiseQueryService.java)
- Para hablar con la persistencia usa interfaces (puertos), así que la aplicación no queda pegada a Mongo:
  - [FranchiseRepositoryPort.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/port/out/FranchiseRepositoryPort.java)
  - [BranchRepositoryPort.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/port/out/BranchRepositoryPort.java)
  - [ProductRepositoryPort.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/application/port/out/ProductRepositoryPort.java)

### 3) Infrastructure (MongoDB y “los cables”)

Acá está todo lo que tiene que ver con MongoDB y Spring Data Reactive. Implementa los puertos de Application y se encarga de traducir lo que guarda Mongo a los modelos del dominio.

- Documentos/colecciones Mongo (DTOs de persistencia).
- Repositorios Spring Data (ReactiveCrudRepository).
- Adaptadores que traducen Document ↔ Domain.

Ejemplos:
- Adaptadores:
  - [FranchiseMongoAdapter.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/infrastructure/mongo/adapter/FranchiseMongoAdapter.java)
  - [BranchMongoAdapter.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/infrastructure/mongo/adapter/BranchMongoAdapter.java)
  - [ProductMongoAdapter.java](file:///c:/Users/Aurik3/Desktop/PROYECTS/entrevista%20springboot/src/main/java/com/franchise/api/infrastructure/mongo/adapter/ProductMongoAdapter.java)

Modelo de datos (colecciones separadas para que sea simple de consultar y mantener):

- `franchises` (`id`, `name`)
- `branches` (`id`, `franchiseId`, `name`)
- `products` (`id`, `branchId`, `name`, `stock`)

### 4) Presentation (la API)

Es la capa que recibe los requests HTTP, valida lo que llega, transforma IDs, y delega el trabajo a Application. También es donde se define el “contrato” (DTOs de request/response).

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

1. Llega el request a `PATCH /api/branches/{branchId}/products/{productId}/stock`.
2. El controlador valida el body y convierte los IDs a UUID.
3. Application ejecuta el caso de uso y se asegura de que el producto realmente pertenezca a esa sucursal.
4. Infrastructure actualiza en Mongo y devuelve el producto actualizado.
5. Se responde el JSON final.

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
