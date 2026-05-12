# Documento Técnico — Prueba Backend (Spring Boot)

## 1. Objetivo

Construir un API para gestionar franquicias, sucursales y productos con stock, cumpliendo:

- Programación reactiva
- Pruebas unitarias
- Contenerización con Docker
- Infrastructure as Code (IaC)
- Estructura basada en Clean Architecture
- Buenas prácticas (legibilidad, mantenibilidad, organización)

## 2. Alcance funcional (requisitos)

Entidades:

- Franquicia: nombre, lista de sucursales
- Sucursal: nombre, lista de productos
- Producto: nombre, cantidad de stock

Endpoints mínimos:

- Crear franquicia
- Agregar sucursal a una franquicia
- Agregar producto a una sucursal
- Eliminar producto de una sucursal
- Modificar stock de un producto
- Consultar, para una franquicia, el producto con mayor stock por cada sucursal (retornando lista de productos indicando a qué sucursal pertenecen)

Extras:

- Actualizar nombre de franquicia
- Actualizar nombre de sucursal
- Actualizar nombre de producto

Persistencia:

- Usar un sistema como MongoDB / MySQL / Redis / Dynamo, etc. (se elige una opción)

## 3. Decisiones técnicas (propuesta)

### 3.1 Stack

- Java 17+
- Spring Boot 3.x
- Spring WebFlux (reactivo)
- Persistencia: MongoDB con driver reactivo (Spring Data Reactive MongoDB)
- Validación: Jakarta Validation
- Tests: JUnit 5, Reactor Test, Mockito, Spring Boot Test
- Tests de integración: Testcontainers (MongoDB)
- Documentación de API: OpenAPI 3 (Springdoc) + Swagger UI
- Contenerización: Docker + Docker Compose
- IaC: Terraform (provider Docker) para aprovisionar la infraestructura local (MongoDB y red)

Motivo de elección de MongoDB:

- Modelo flexible para franquicias/sucursales/productos.
- Soporte reactivo maduro con Spring Data.
- Fácil de levantar localmente con Docker.

### 3.2 Documentación de endpoints (Swagger/OpenAPI)

Se implementará documentación navegable de los endpoints usando OpenAPI 3, con soporte para Spring WebFlux.

- Librería: Springdoc OpenAPI para WebFlux (starter con UI).
- Rutas esperadas:
  - OpenAPI JSON: `GET /v3/api-docs`
  - Swagger UI: `GET /swagger-ui.html` (redirige a WebJars)
- Estrategia:
  - Definir metadata global (título, versión, descripción).
  - Anotar endpoints críticos con `@Operation`/`@ApiResponses` y modelar DTOs con `@Schema` cuando aporte claridad.
  - Restringir la UI por perfil (habilitada en `local`/`dev` y deshabilitada en `prod`).

## 4. Arquitectura (Clean Architecture)

La solución se organizará por capas con dependencias unidireccionales hacia el dominio:

- **Domain**
  - Entidades y reglas de negocio (Franchise, Branch, Product)
  - Value objects (por ejemplo, identificadores y nombres validados)
  - Excepciones de dominio (por ejemplo, “Franquicia no existe”)
- **Application**
  - Casos de uso (create franchise, add branch, add product, delete product, update stock, query max stock por sucursal, updates de nombres)
  - Puertos (interfaces) de salida: repositorios/DAO, reloj si aplica
  - Puertos (interfaces) de entrada: servicios/use cases expuestos a la capa web
- **Infrastructure**
  - Implementaciones de persistencia (Mongo repositories + mappers)
  - Configuración de Mongo (URI, índices si se definen)
- **Presentation**
  - Controllers/Handlers WebFlux + DTOs (requests/responses)
  - Manejo uniforme de errores (Problem Details o estructura consistente)
  - Validación de requests (anotaciones y/o validadores)

Reglas:

- Presentation depende de Application.
- Application depende de Domain.
- Infrastructure depende de Application + Domain (implementa puertos).
- Domain no depende de nada.

## 5. Modelo de datos (propuesta)

### 5.1 Identificadores

- Todos los recursos se manejarán con `UUID` como id público.

### 5.2 Colecciones

Se propone normalizar para simplificar operaciones y consultas:

- `franchises`
  - `_id` (UUID)
  - `name`
- `branches`
  - `_id` (UUID)
  - `franchiseId` (UUID)
  - `name`
- `products`
  - `_id` (UUID)
  - `branchId` (UUID)
  - `name`
  - `stock` (long/int)

Ventajas:

- Agregar/eliminar producto no requiere reescritura de documentos grandes.
- La consulta “máximo stock por sucursal” se resuelve con agregación o consultas por branch.

Índices recomendados:

- `branches(franchiseId)`
- `products(branchId)`
- Opcional: unicidad por contexto
  - `franchises(name)` único (si se desea)
  - `branches(franchiseId, name)` único
  - `products(branchId, name)` único

## 6. Diseño de API (contracto propuesto)

Base path:

- `/api`

### 6.1 Crear franquicia

- `POST /api/franchises`

Request:

```json
{ "name": "Franquicia A" }
```

Response `201`:

```json
{ "id": "uuid", "name": "Franquicia A" }
```

### 6.2 Actualizar nombre de franquicia (extra)

- `PATCH /api/franchises/{franchiseId}`

Request:

```json
{ "name": "Nuevo nombre" }
```

Response `200`

### 6.3 Agregar sucursal

- `POST /api/franchises/{franchiseId}/branches`

Request:

```json
{ "name": "Sucursal 1" }
```

Response `201`:

```json
{ "id": "uuid", "franchiseId": "uuid", "name": "Sucursal 1" }
```

### 6.4 Actualizar nombre de sucursal (extra)

- `PATCH /api/branches/{branchId}`

Request:

```json
{ "name": "Nuevo nombre sucursal" }
```

### 6.5 Agregar producto a sucursal

- `POST /api/branches/{branchId}/products`

Request:

```json
{ "name": "Producto X", "stock": 10 }
```

Response `201`:

```json
{ "id": "uuid", "branchId": "uuid", "name": "Producto X", "stock": 10 }
```

### 6.6 Eliminar producto de una sucursal

- `DELETE /api/branches/{branchId}/products/{productId}`

Response `204`

### 6.7 Modificar stock de un producto

- `PATCH /api/branches/{branchId}/products/{productId}/stock`

Request:

```json
{ "stock": 25 }
```

Response `200`:

```json
{ "id": "uuid", "branchId": "uuid", "name": "Producto X", "stock": 25 }
```

### 6.8 Actualizar nombre de producto (extra)

- `PATCH /api/branches/{branchId}/products/{productId}`

Request:

```json
{ "name": "Nuevo nombre producto" }
```

### 6.9 Producto con mayor stock por sucursal (para una franquicia)

- `GET /api/franchises/{franchiseId}/branches/max-stock-products`

Response `200` (uno por sucursal):

```json
[
  {
    "branchId": "uuid",
    "branchName": "Sucursal 1",
    "product": { "id": "uuid", "name": "Producto A", "stock": 100 }
  },
  {
    "branchId": "uuid",
    "branchName": "Sucursal 2",
    "product": { "id": "uuid", "name": "Producto Z", "stock": 50 }
  }
]
```

Notas:

- Si una sucursal no tiene productos, se define comportamiento:
  - Omitirla del listado, o retornar `product: null`. Se elegirá “omitir” para mantener respuesta limpia.
- Validaciones:
  - `name` requerido, trim, longitud razonable.
  - `stock` >= 0.

## 7. Manejo de errores (propuesta)

Respuesta consistente para errores:

- `400` validación (campos inválidos)
- `404` recurso no existe
- `409` conflicto (por ejemplo, nombres duplicados si se activa unicidad)

Formato sugerido:

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Branch not found",
  "details": { "branchId": "..." }
}
```

## 8. Plan de implementación paso a paso

### Paso 1 — Inicialización del proyecto

- Crear proyecto Spring Boot con:
  - WebFlux
  - Reactive MongoDB
  - Validation
  - Actuator (opcional)
  - Test (JUnit 5)
- Definir propiedades por ambiente:
  - `application.yml` base
  - `application-local.yml` (Mongo local)
  - `application-test.yml` (Testcontainers o configuración test)

### Paso 2 — Estructura Clean Architecture

- Crear paquetes/módulos internos:
  - `domain`
  - `application`
  - `infrastructure`
  - `presentation`
- Definir entidades del dominio y reglas de negocio mínimas.

### Paso 3 — Puertos y casos de uso

- Definir interfaces (puertos) para:
  - FranchiseRepositoryPort
  - BranchRepositoryPort
  - ProductRepositoryPort
- Implementar casos de uso:
  - CreateFranchise
  - RenameFranchise
  - AddBranchToFranchise
  - RenameBranch
  - AddProductToBranch
  - DeleteProductFromBranch
  - UpdateProductStock
  - RenameProduct
  - GetMaxStockProductPerBranchByFranchise

### Paso 4 — Adaptador de persistencia (Mongo)

- Crear documentos Mongo para cada entidad (o mapping 1:1).
- Implementar repositorios reactivos.
- Implementar mapeos entre Domain y Document.

### Paso 5 — Capa web (WebFlux)

- Definir DTOs para requests/responses.
- Implementar endpoints (Controllers o functional routing).
- Implementar validación y handler global de errores.
- Integrar OpenAPI/Swagger UI y documentar los endpoints principales.

### Paso 6 — Pruebas

- Unit tests (rápidas, sin I/O):
  - Casos de uso con repositorios mockeados (Mockito)
  - Validación de reglas del dominio
  - Errores de negocio (conflictos, no encontrados)
- Integration tests:
  - Con Testcontainers MongoDB para el adaptador de persistencia
  - Web layer tests (WebTestClient) para endpoints críticos

### Paso 7 — Docker

- Dockerfile multi-stage para empaquetar el jar.
- docker-compose para levantar:
  - API
  - MongoDB

### Paso 8 — IaC con Terraform (local)

- Terraform con provider Docker para levantar:
  - network
  - container MongoDB
  - (opcional) container para mongo-express
- Variables para puertos, credenciales y nombres.

El objetivo es que cualquier evaluador pueda ejecutar:

- `terraform init`
- `terraform apply`

y tener la persistencia lista sin instalar Mongo manualmente.

## 9. Requisitos de instalación (local)

En Windows se recomienda tener estas versiones o superiores:

- Git
- JDK 21 (JAVA_HOME configurado)
- Maven 3.9+ (o usar Maven Wrapper si se incluye)
- Docker Desktop (con WSL2 habilitado)
- Terraform 1.6+
- Herramienta para pruebas HTTP:
  - Postman o Insomnia, o `curl`

Opcional (útil):

- IntelliJ IDEA / VS Code

## 10. Ejecución local (modo desarrollo)

### 10.1 Levantar persistencia (opción Docker Compose)

Comandos esperados:

```bash
docker compose up -d
```

Verificación:

- Mongo escuchando en el puerto configurado (por defecto 27017).

### 10.2 Ejecutar la API

Comandos esperados:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Verificación:

- Healthcheck (si se habilita Actuator):
  - `GET /actuator/health`
- OpenAPI/Swagger (si se habilita en el perfil):
  - `GET /v3/api-docs`
  - `GET /swagger-ui.html`

## 11. Ejecución con Terraform (IaC local)

Requisitos:

- Docker Desktop corriendo
- Terraform instalado

Comandos esperados:

```bash
terraform init
terraform apply
```

Verificación:

- Contenedor de Mongo levantado por Terraform.
- Variables de conexión disponibles para la API (URI).

## 12. Pruebas automáticas

### 12.1 Unit tests

Comandos esperados:

```bash
./mvnw test
```

Cobertura objetivo (orientativo):

- Casos de uso principales con caminos felices y de error.

### 12.2 Integration tests (Testcontainers)

Se ejecutan dentro de `./mvnw test` cuando estén configuradas:

- Requieren Docker corriendo.

## 13. Pruebas de funcionalidad (paso a paso con curl)

Se asume API en `http://localhost:8080/api`.

### 13.1 Crear franquicia

```bash
curl -i -X POST "http://localhost:8080/api/franchises" ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Franquicia A\"}"
```

Guardar `franchiseId` del response.

### 13.2 Crear sucursal

```bash
curl -i -X POST "http://localhost:8080/api/franchises/{franchiseId}/branches" ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Sucursal 1\"}"
```

Guardar `branchId`.

### 13.3 Agregar productos

```bash
curl -i -X POST "http://localhost:8080/api/branches/{branchId}/products" ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Producto A\",\"stock\":10}"
```

```bash
curl -i -X POST "http://localhost:8080/api/branches/{branchId}/products" ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Producto B\",\"stock\":50}"
```

Guardar `productId` de alguno.

### 13.4 Actualizar stock

```bash
curl -i -X PATCH "http://localhost:8080/api/branches/{branchId}/products/{productId}/stock" ^
  -H "Content-Type: application/json" ^
  -d "{\"stock\":80}"
```

### 13.5 Consultar máximo stock por sucursal para la franquicia

```bash
curl -i "http://localhost:8080/api/franchises/{franchiseId}/branches/max-stock-products"
```

Debe retornar, para cada sucursal, el producto con mayor stock y a qué sucursal pertenece.

### 13.6 Eliminar producto

```bash
curl -i -X DELETE "http://localhost:8080/api/branches/{branchId}/products/{productId}"
```

### 13.7 Pruebas de extras (renombrar)

Franquicia:

```bash
curl -i -X PATCH "http://localhost:8080/api/franchises/{franchiseId}" ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Franquicia Renombrada\"}"
```

Sucursal:

```bash
curl -i -X PATCH "http://localhost:8080/api/branches/{branchId}" ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Sucursal Renombrada\"}"
```

Producto:

```bash
curl -i -X PATCH "http://localhost:8080/api/branches/{branchId}/products/{productId}" ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Producto Renombrado\"}"
```

## 14. Checklist de calidad (antes de entregar)

- Endpoints cumplen criterios de aceptación.
- Programación reactiva end-to-end (Flux/Mono) sin bloqueos.
- Pruebas unitarias cubren casos relevantes y fallan si hay regresiones.
- Manejo de errores consistente.
- Docker build y docker compose funcionan desde cero.
- Terraform apply levanta persistencia local sin pasos manuales.
- Variables y secretos no se versionan (usar `.env`/variables de entorno).
- Formato de código consistente.
- Swagger UI disponible en `local` y no expuesta en `prod`.
