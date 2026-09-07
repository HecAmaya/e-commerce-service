# E-commerce Service

Aplicación local de catálogo y compras simuladas. Permite administrar productos, importarlos desde CSV, buscarlos y crear órdenes con descuento transaccional de inventario.

## Tecnologías

- Java 21, Spring Boot 3.5, Spring Data JPA
- PostgreSQL 17
- React 19, TypeScript, Vite y Nginx
- Docker Compose

## Ejecutar con Docker

Requisitos: Docker Desktop.

```bash
docker compose up --build
```

URLs:

- Aplicación: http://localhost:3000
- API: http://localhost:8080

Detener:

```bash
docker compose down
```

Para borrar también los datos locales:

```bash
docker compose down -v
```

## Ejecutar en desarrollo

Requisitos: JDK 21, Maven 3.9+, Node.js 20+ y npm.

1. Iniciar PostgreSQL:

```bash
docker compose up postgres
```

2. Iniciar el backend desde la raíz:

```bash
mvn spring-boot:run
```

3. Iniciar el frontend:

```bash
cd frontend
npm install
npm run dev
```

Frontend de desarrollo: http://localhost:5173

## API

### Listar y buscar productos

```bash
curl "http://localhost:8080/api/products"
curl "http://localhost:8080/api/products?q=shoe"
curl "http://localhost:8080/api/products/1"
```

La búsqueda revisa nombre, SKU, descripción y categoría.

### Crear producto

```bash
curl -X POST http://localhost:8080/api/products ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Running Shoes\",\"sku\":\"RS-001\",\"description\":\"Daily training shoes\",\"category\":\"Footwear\",\"price\":89.99,\"stock\":150,\"weightKg\":0.35}"
```

### Actualizar y eliminar producto

```bash
curl -X PUT http://localhost:8080/api/products/1 ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Updated Shoes\",\"sku\":\"RS-001\",\"description\":\"Updated description\",\"category\":\"Footwear\",\"price\":94.99,\"stock\":100,\"weightKg\":0.35}"

curl -X DELETE http://localhost:8080/api/products/1
```

### Importar CSV

Encabezados requeridos:

```text
name,sku,description,category,price,stock,weight_kg
```

```bash
curl -X POST http://localhost:8080/api/products/import ^
  -F "file=@data/sample/NTD-Code-Challenge-E-Commerce.csv"
```

Las filas válidas se importan y las inválidas se reportan con número de fila y motivo. El archivo de ejemplo fue descargado el **2026-09-02**.

### Comprar producto

```bash
curl -X POST http://localhost:8080/api/orders ^
  -H "Content-Type: application/json" ^
  -d "{\"productId\":1,\"quantity\":2}"
```

La compra usa una transacción y bloqueo pesimista para evitar vender más unidades que las disponibles. La respuesta incluye el total y el detalle de la orden. La falta de inventario devuelve `409 Conflict`.

## Pruebas

Backend:

```bash
mvn test
```

Frontend:

```bash
cd frontend
npm run build
```
