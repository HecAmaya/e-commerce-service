# Commerce Console

Enterprise-style product catalog and fake-purchase application built with Java 21, Spring Boot, PostgreSQL, and React/TypeScript (Vite).

## Decisions and approach

- Spring Data JPA provides durable product and order persistence; PostgreSQL is the local production-like database.
- Prices and weights use `BigDecimal`, and SKU uniqueness is enforced both in the service and database.
- Purchase uses a pessimistic row lock inside a transaction so concurrent requests cannot oversell stock. Orders store a snapshot of item name, SKU, price, and quantity.
- CSV imports are deliberately strict: values are trimmed, validated, and rejected with row number and reason. Currency strings such as `$29.99` and non-numeric values such as `free` are reported rather than silently normalized.
- Every CSV field in the proposed structure is required, including `sku`; invalid rows are rejected without being persisted.
- The frontend is a small responsive operations console. Nginx serves the compiled Vite app and proxies `/api` to the backend in Compose.

Alternatives considered: a document database was rejected because inventory decrement and relational order history benefit from transactions; a hosted payment provider was out of scope for the requested fake flow; a CSV library was chosen over hand-written parsing to support quoted commas safely.

## Local run

Requirements: JDK 21, Maven 3.9+, Node 20+, npm, and Docker Desktop.

Start the complete stack:

```text
docker compose up --build
```

Open `http://localhost:3000`. The API is at `http://localhost:8080`; PostgreSQL is available inside Compose at port 5432. Stop with `docker compose down` (add `-v` to remove database data).

For development, start PostgreSQL with `docker compose up postgres`, then run `mvn spring-boot:run` from the root and `npm install && npm run dev` from `frontend`. The Vite development UI is at `http://localhost:5173`.

The sample file is included at `data/sample/NTD-Code-Challenge-E-Commerce.csv` and was downloaded/added on **2026-09-02**.

The repository currently contains the complete working tree but is not connected to a GitHub remote in this environment. Publish it to the target GitHub repository before submitting the challenge.

## API

`GET /api/products?q=shoe` searches name, SKU, description, and category. `GET /api/products/{id}` retrieves one product.

`POST /api/products` and `PUT /api/products/{id}` accept JSON with `name`, `sku`, `description`, `category`, `price`, `stock`, and `weightKg`. `DELETE /api/products/{id}` removes a product.

`POST /api/products/import` accepts a multipart `file` containing headers `name,sku,description,category,price,stock,weight_kg`. It returns `{ imported, rejected, errors: [{ row, sku, reason }] }`; valid rows are committed while malformed rows are reported.

`POST /api/orders` accepts `{ "productId": 1, "quantity": 2 }`, decrements stock atomically, and returns an order confirmation with total and item snapshot. Insufficient stock returns HTTP 409.

## Verification

Backend tests cover strict CSV reporting, stock decrement, and insufficient-stock protection:

```text
mvn test
```

Build the frontend with `npm run build` from `frontend`.

The backend test suite and Docker Compose configuration were validated locally. The frontend build requires Node.js/npm to be installed; npm was not available in the review environment, so that command must still be run in a Node-enabled environment before submission.
